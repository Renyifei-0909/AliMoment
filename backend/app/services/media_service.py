from __future__ import annotations

import shutil
import subprocess
import uuid
import json
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional

from app.config import settings
from app.integrations.narrato.compose_adapter import NarratoComposeError, get_compose_adapter


class MediaServiceError(RuntimeError):
    pass


@dataclass
class UploadedMedia:
    media_id: str
    filename: str
    original_filename: str
    file_size: int
    path: Path
    preview_filename: str
    preview_relative_url: str
    preview_url: str
    preview_note: str


@dataclass
class EditResult:
    media_id: str
    output_filename: str
    output_relative_url: str
    output_url: str
    speed: float
    effect_intensity: float
    voiceover_requested: bool
    voiceover_applied: bool
    note: str


class MediaService:
    def __init__(self) -> None:
        self._inputs_dir = Path(settings.storage_inputs_dir)
        self._previews_dir = Path(settings.storage_previews_dir)
        self._outputs_dir = Path(settings.storage_outputs_dir)

    def save_upload(self, *, original_filename: str, content: bytes) -> UploadedMedia:
        if not content:
            raise MediaServiceError("上传文件为空。")

        safe_name = Path(original_filename or "upload.mp4").name
        suffix = Path(safe_name).suffix or ".mp4"
        media_id = f"{uuid.uuid4().hex}{suffix.lower()}"
        target = self._inputs_dir / media_id
        target.write_bytes(content)
        preview_filename, preview_relative_url, preview_note = self._ensure_preview_proxy(
            media_id=media_id,
            input_path=target,
        )

        return UploadedMedia(
            media_id=media_id,
            filename=media_id,
            original_filename=safe_name,
            file_size=len(content),
            path=target,
            preview_filename=preview_filename,
            preview_relative_url=preview_relative_url,
            preview_url=self._build_public_url(preview_relative_url),
            preview_note=preview_note,
        )

    def edit(
        self,
        *,
        media_id: str,
        segments: List[dict],
        speed: float,
        effect_intensity: float,
        add_voiceover: bool,
    ) -> EditResult:
        if settings.edit_engine.lower() == "narrato":
            return self._edit_with_narrato(
                media_id=media_id,
                segments=segments,
                speed=speed,
                effect_intensity=effect_intensity,
                add_voiceover=add_voiceover,
            )

        ffmpeg_bin = shutil.which("ffmpeg")
        input_path = self._resolve_media_path(media_id)
        if not ffmpeg_bin:
            return self._fallback_copy_output(
                media_id=media_id,
                input_path=input_path,
                speed=speed,
                effect_intensity=effect_intensity,
                add_voiceover=add_voiceover,
            )

        temp_dir = self._outputs_dir / f"tmp_{uuid.uuid4().hex}"
        temp_dir.mkdir(parents=True, exist_ok=True)

        output_filename = f"edit_{uuid.uuid4().hex}.mp4"
        output_path = self._outputs_dir / output_filename

        try:
            segment_files: List[Path] = []
            for index, segment in enumerate(segments, start=1):
                start = float(segment["start"])
                end = float(segment["end"])
                if end <= start:
                    raise MediaServiceError("剪辑片段的结束时间必须大于开始时间。")

                segment_output = temp_dir / f"segment_{index:02d}.mp4"
                self._run_ffmpeg_segment(
                    ffmpeg_bin=ffmpeg_bin,
                    input_path=input_path,
                    output_path=segment_output,
                    start=start,
                    end=end,
                    speed=speed,
                    effect_intensity=effect_intensity,
                )
                segment_files.append(segment_output)

            if len(segment_files) == 1:
                shutil.move(str(segment_files[0]), str(output_path))
            else:
                concat_file = temp_dir / "concat.txt"
                concat_file.write_text(
                    "\n".join(f"file '{path.name}'" for path in segment_files),
                    encoding="utf-8",
                )
                self._run_subprocess(
                    [
                        ffmpeg_bin,
                        "-y",
                        "-f",
                        "concat",
                        "-safe",
                        "0",
                        "-i",
                        str(concat_file),
                        "-c",
                        "copy",
                        str(output_path),
                    ],
                    cwd=temp_dir,
                )
        finally:
            shutil.rmtree(temp_dir, ignore_errors=True)

        relative_url = f"/media/outputs/{output_filename}"
        note = "已生成无旁白版本。" if add_voiceover else ""
        if add_voiceover:
            note = "当前版本未接入 TTS，已先生成无旁白版本。"

        return EditResult(
            media_id=media_id,
            output_filename=output_filename,
            output_relative_url=relative_url,
            output_url=settings.backend_public_base_url.rstrip("/") + relative_url,
            speed=speed,
            effect_intensity=effect_intensity,
            voiceover_requested=add_voiceover,
            voiceover_applied=False,
            note=note,
        )

    def _edit_with_narrato(
        self,
        *,
        media_id: str,
        segments: List[dict],
        speed: float,
        effect_intensity: float,
        add_voiceover: bool,
    ) -> EditResult:
        input_path = self._resolve_media_path(media_id)
        script_path = self._build_narrato_script(media_id=media_id, segments=segments)
        narrato_task_id = f"edit_{uuid.uuid4().hex}"

        params = {
            "video_clip_json_path": str(script_path),
            "video_origin_path": str(input_path),
            "tts_engine": settings.narrato_tts_engine,
            "voice_name": settings.narrato_voice_name,
            "voice_rate": 1.0,
            "voice_pitch": 1.0,
            "subtitle_enabled": False,
            "font_name": "Microsoft YaHei",
            "font_size": 24,
            "text_fore_color": "#FFFFFF",
            "subtitle_position": "bottom",
            "custom_position": 70.0,
            "n_threads": settings.narrato_n_threads,
            "video_aspect": "16:9",
            "tts_volume": 1.0,
            "original_volume": 1.0,
            "bgm_volume": 0.0,
        }

        try:
            result = get_compose_adapter().run_compose_blocking(task_id=narrato_task_id, params=params)
        except NarratoComposeError as exc:
            raise MediaServiceError(f"Narrato 合成失败: {exc}") from exc

        output_urls = result.get("output_urls") or []
        copied_paths = result.get("videos") or []
        if not output_urls or not copied_paths:
            raise MediaServiceError("Narrato 未返回可用输出视频")

        output_filename = Path(copied_paths[0]).name
        output_relative_url = f"/media/outputs/{output_filename}"
        note = (
            "已通过 Narrato 引擎生成视频。"
            " 当前参数 speed/effect_intensity 由 Narrato 流程忽略。"
        )
        if add_voiceover:
            note += " add_voiceover 已透传 Narrato 默认语音策略。"

        return EditResult(
            media_id=media_id,
            output_filename=output_filename,
            output_relative_url=output_relative_url,
            output_url=output_urls[0],
            speed=speed,
            effect_intensity=effect_intensity,
            voiceover_requested=add_voiceover,
            voiceover_applied=add_voiceover,
            note=note,
        )

    def _build_narrato_script(self, *, media_id: str, segments: List[dict]) -> Path:
        script_items = []
        for index, segment in enumerate(segments, start=1):
            start = float(segment["start"])
            end = float(segment["end"])
            if end <= start:
                raise MediaServiceError("剪辑片段的结束时间必须大于开始时间。")
            script_items.append(
                {
                    "_id": index,
                    "timestamp": f"{self._seconds_to_srt_time(start)}-{self._seconds_to_srt_time(end)}",
                    "picture": "",
                    "narration": "",
                    "OST": 1,
                }
            )

        script_path = Path(settings.storage_features_dir) / f"narrato_script_{media_id}_{uuid.uuid4().hex}.json"
        script_path.parent.mkdir(parents=True, exist_ok=True)
        script_path.write_text(json.dumps(script_items, ensure_ascii=False, indent=2), encoding="utf-8")
        return script_path

    @staticmethod
    def _seconds_to_srt_time(seconds: float) -> str:
        millis = int(round(seconds * 1000))
        hours, rem = divmod(millis, 3600000)
        minutes, rem = divmod(rem, 60000)
        secs, ms = divmod(rem, 1000)
        return f"{hours:02d}:{minutes:02d}:{secs:02d},{ms:03d}"

    def _fallback_copy_output(
        self,
        *,
        media_id: str,
        input_path: Path,
        speed: float,
        effect_intensity: float,
        add_voiceover: bool,
    ) -> EditResult:
        output_filename = f"edit_fallback_{uuid.uuid4().hex}{input_path.suffix or '.mp4'}"
        output_path = self._outputs_dir / output_filename
        shutil.copy2(input_path, output_path)
        relative_url = f"/media/outputs/{output_filename}"
        return EditResult(
            media_id=media_id,
            output_filename=output_filename,
            output_relative_url=relative_url,
            output_url=settings.backend_public_base_url.rstrip("/") + relative_url,
            speed=speed,
            effect_intensity=effect_intensity,
            voiceover_requested=add_voiceover,
            voiceover_applied=False,
            note="当前服务器未安装 ffmpeg，已降级返回原视频副本用于演示链路验证。",
        )

    def _ensure_preview_proxy(self, *, media_id: str, input_path: Path) -> tuple[str, str, str]:
        ffmpeg_bin = shutil.which("ffmpeg")
        if not ffmpeg_bin:
            relative_url = f"/media/inputs/{media_id}"
            return media_id, relative_url, "当前服务器未安装 ffmpeg，预览先回退为原始上传文件。"

        preview_filename = f"preview_{Path(media_id).stem}.mp4"
        output_path = self._previews_dir / preview_filename
        command = [
            ffmpeg_bin,
            "-y",
            "-i",
            str(input_path),
            "-vf",
            "scale='min(1280,iw)':-2:force_original_aspect_ratio=decrease",
            "-c:v",
            "libx264",
            "-preset",
            "veryfast",
            "-crf",
            "23",
            "-c:a",
            "aac",
            "-movflags",
            "+faststart",
            str(output_path),
        ]
        try:
            self._run_subprocess(command)
        except MediaServiceError:
            relative_url = f"/media/inputs/{media_id}"
            return media_id, relative_url, "代理预览生成失败，已回退为原始上传文件。"

        return preview_filename, f"/media/previews/{preview_filename}", "已生成可预览代理文件。"

    def _resolve_media_path(self, media_id: str) -> Path:
        filename = Path(media_id).name
        path = self._inputs_dir / filename
        if not path.exists():
            raise MediaServiceError(f"找不到上传视频：{media_id}")
        return path

    @staticmethod
    def _build_public_url(relative_url: str) -> str:
        return settings.backend_public_base_url.rstrip("/") + relative_url

    @staticmethod
    def _run_ffmpeg_segment(
        *,
        ffmpeg_bin: str,
        input_path: Path,
        output_path: Path,
        start: float,
        end: float,
        speed: float,
        effect_intensity: float,
    ) -> None:
        vf_parts = []
        af_parts = []

        if abs(speed - 1.0) > 1e-4:
            vf_parts.append(f"setpts={1.0 / speed:.6f}*PTS")
            af_parts.extend(MediaService._build_atempo_filters(speed))

        if effect_intensity > 0:
            normalized = max(0.0, min(1.0, effect_intensity / 100.0))
            saturation = max(0.0, 1.0 - 0.65 * normalized)
            contrast = 1.0 + 0.2 * normalized
            brightness = 0.08 * normalized
            vf_parts.append(
                f"eq=saturation={saturation:.3f}:contrast={contrast:.3f}:brightness={brightness:.3f}"
            )

        command = [
            ffmpeg_bin,
            "-y",
            "-ss",
            f"{start:.3f}",
            "-to",
            f"{end:.3f}",
            "-i",
            str(input_path),
        ]
        if vf_parts:
            command.extend(["-vf", ",".join(vf_parts)])
        if af_parts:
            command.extend(["-af", ",".join(af_parts)])
        command.extend(
            [
                "-c:v",
                "libx264",
                "-preset",
                "veryfast",
                "-crf",
                "23",
                "-c:a",
                "aac",
                "-movflags",
                "+faststart",
                str(output_path),
            ]
        )
        MediaService._run_subprocess(command)

    @staticmethod
    def _build_atempo_filters(speed: float) -> List[str]:
        filters: List[str] = []
        remaining = speed
        while remaining > 2.0:
            filters.append("atempo=2.0")
            remaining /= 2.0
        while remaining < 0.5:
            filters.append("atempo=0.5")
            remaining /= 0.5
        filters.append(f"atempo={remaining:.6f}")
        return filters

    @staticmethod
    def _run_subprocess(command: List[str], cwd: Optional[Path] = None) -> None:
        try:
            subprocess.run(
                command,
                cwd=str(cwd) if cwd else None,
                check=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
        except subprocess.CalledProcessError as exc:
            message = exc.stderr.strip() or exc.stdout.strip() or "ffmpeg 执行失败。"
            raise MediaServiceError(message) from exc


_media_service: Optional[MediaService] = None


def get_media_service() -> MediaService:
    global _media_service
    if _media_service is None:
        _media_service = MediaService()
    return _media_service
