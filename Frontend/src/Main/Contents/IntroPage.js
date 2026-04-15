import './IntroPage.css';


export default function Intro({ pagenum }) {
    let containerClass = 'Intromain';

    if (pagenum > 0) {
        containerClass = 'Intromain';
    } else if (pagenum === 0) {
        containerClass = 'Intromainup';
    }

    return (
        <div className={containerClass}>
            <div className="intro-grid">
                <div className="intro-left">
                    <div className="card card-top">
                        <h3 className="card-title">概览</h3>
                        <p className="card-text">欢迎来到发布博客主页，这里展示近期动态与导航。</p>
                    </div>

                    <div className="card-2x2">
                        <div className="card card-small">
                            <h4 className="card-title">最新发布</h4>
                            <p className="card-text">最近的版本更新内容概览。</p>
                        </div>
                        <div className="card card-small">
                            <h4 className="card-title">热门文章</h4>
                            <p className="card-text">社区关注的主题与讨论。</p>
                        </div>
                        <div className="card card-small">
                            <h4 className="card-title">快速开始</h4>
                            <p className="card-text">新手指南与上手教程。</p>
                        </div>
                        <div className="card card-small">
                            <h4 className="card-title">资源链接</h4>
                            <p className="card-text">文档、示例与工具集合。</p>
                        </div>
                    </div>
                </div>

                <div className="intro-right">
                    <div className="card card-large">
                        <h3 className="card-title">主内容</h3>
                        <p className="card-text">
                            这里是一张较大的卡片，用于展示主要内容、公告或精选文章摘要。
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}