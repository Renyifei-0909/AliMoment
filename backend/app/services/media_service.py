from __future__ import annotations

import shutil
import subprocess
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional

from app.config import settings


class MediaServiceError(RuntimeError):
    pass


@dataclass
class UploadedMedia:
    media_id: str
    filename: str
    original_filename: str
    file_size: int
    path: Path


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
        self._outputs_dir = Path(settings.storage_outputs_dir)

    def save_upload(self, *, original_filename: str, content: bytes) -> UploadedMedia:
        if not content:
            raise MediaServiceError("上传文件为空。")

        safe_name = Path(original_filename or "upload.mp4").name
        suffix = Path(safe_name).suffix or ".mp4"
        media_id = f"{uuid.uuid4().hex}{suffix.lower()}"
        target = self._inputs_dir / media_id
        target.write_bytes(content)

        return UploadedMedia(
            media_id=media_id,
            filename=media_id,
            original_filename=safe_name,
            file_size=len(content),
            path=target,
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

    def _resolve_media_path(self, media_id: str) -> Path:
        filename = Path(media_id).name
        path = self._inputs_dir / filename
        if not path.exists():
            raise MediaServiceError(f"找不到上传视频：{media_id}")
        return path

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
