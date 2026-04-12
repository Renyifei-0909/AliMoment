// 页面切换功能
document.addEventListener('DOMContentLoaded', () => {
    const navItems = document.querySelectorAll('.nav-item');
    const pages = document.querySelectorAll('.page');

    // 导航点击事件
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const targetPage = item.getAttribute('data-page');
            
            // 移除所有活动状态
            navItems.forEach(nav => nav.classList.remove('active'));
            pages.forEach(page => page.classList.remove('active'));
            
            // 添加当前活动状态
            item.classList.add('active');
            const activePage = document.getElementById(`page-${targetPage}`);
            if (activePage) {
                activePage.classList.add('active');
            }
            
            // 添加点击动画效果
            item.style.transform = 'scale(0.95)';
            setTimeout(() => {
                item.style.transform = '';
            }, 150);
        });
    });

    // 窗口控制按钮
    const minimizeBtn = document.getElementById('minimizeBtn');
    const maximizeBtn = document.getElementById('maximizeBtn');
    const closeBtn = document.getElementById('closeBtn');

    if (minimizeBtn) {
        minimizeBtn.addEventListener('click', async () => {
            if (window.electronAPI) {
                await window.electronAPI.minimizeWindow();
            }
        });
    }

    if (maximizeBtn) {
        maximizeBtn.addEventListener('click', async () => {
            if (window.electronAPI) {
                await window.electronAPI.maximizeWindow();
            }
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', async () => {
            if (window.electronAPI) {
                await window.electronAPI.closeWindow();
            }
        });
    }

    // 键盘快捷键
    document.addEventListener('keydown', (e) => {
        // Ctrl/Cmd + 数字键切换页面
        if ((e.ctrlKey || e.metaKey) && e.key >= '1' && e.key <= '4') {
            const index = parseInt(e.key) - 1;
            if (navItems[index]) {
                navItems[index].click();
            }
        }
    });
});

// 预加载脚本注入的 API
if (typeof require !== 'undefined') {
    const { ipcRenderer } = require('electron');
    
    window.electronAPI = {
        minimizeWindow: () => ipcRenderer.invoke('minimize-window'),
        maximizeWindow: () => ipcRenderer.invoke('maximize-window'),
        closeWindow: () => ipcRenderer.invoke('close-window')
    };
}
