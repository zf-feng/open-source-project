#include "mainwindow.h"
#include "ui_mainwindow.h"
#include "logindialog.h"
#include "registerdialog.h"
#include "admindialog.h"
#include "dbhelper.h"
#include <QMessageBox>
#include <QTextEdit>
#include <QHBoxLayout>
#include <QTimer>
#include <QMenu>
#include <QGuiApplication>
#include <QScreen>
#include <QProgressBar>
#include <QFileDialog>
#include <QFileInfo>
#include <QScrollArea>
#include <QDesktopServices>
#include <QUrl>
#include <algorithm>

// ============================================================
// MainWindow
// ============================================================

MainWindow::MainWindow(QWidget *parent)
    : QMainWindow(parent)
{
    ui = new Ui::MainWindow;
    ui->setupUi(this);

    setWindowTitle("数字人AI视频创作平台");
    setMinimumSize(900, 550);
    resize(1100, 660);

    // 设置导航栏布局：左右等宽拉伸，中间自然宽度，按钮居中
    QHBoxLayout *navL = qobject_cast<QHBoxLayout*>(ui->navWidget->layout());
    if (navL) {
        navL->setStretch(0, 0);  // leftSection: 自然宽度(logo)
        navL->setStretch(1, 1);  // logoSpacer: 拉伸(左)
        navL->setStretch(2, 0);  // navCenterOuter: 自然宽度(按钮)
        navL->setStretch(3, 1);  // rightSection: 拉伸(右)
        navL->setStretch(4, 0);  // navSpacer: 自然宽度
    }

    // 初始化数据库
    m_db = new DbHelper(this);
    if (!m_db->open()) {
        // 数据库连接失败，后续操作会提示
    }

    // Toast 标签（初始隐藏，浮动在主窗口上）
    m_toast = new QLabel(this);
    m_toast->setStyleSheet(
        "QLabel { background-color: #059669; color: white;"
        "  font-size: 14px; font-weight: 500;"
        "  padding: 8px 20px; border-radius: 6px; }"
    );
    m_toast->setAlignment(Qt::AlignCenter);
    m_toast->hide();

    // 填充动态页面内容
    setupStudioPage();

    // 初始显示首页
    showHomePage();

    // 初始化 AI 服务
    m_aiService = new AiService(this);
    connect(m_aiService, &AiService::videoGenerated,
            this, &MainWindow::onVideoGenerated);
    connect(m_aiService, &AiService::taskStatusUpdated,
            this, &MainWindow::onTaskStatusUpdated);
    connect(m_aiService, &AiService::errorOccurred,
            this, &MainWindow::onErrorOccurred);

    // 轮询任务状态定时器
    m_pollTimer = new QTimer(this);
    m_pollTimer->setInterval(1000);
    connect(m_pollTimer, &QTimer::timeout, this, &MainWindow::pollTaskStatus);

    // 信号连接
    connect(ui->btnHome, &QPushButton::clicked, this, &MainWindow::showHomePage);
    connect(ui->btnStudio, &QPushButton::clicked, this, &MainWindow::showStudioPage);
    connect(ui->btnLogin, &QPushButton::clicked, this, &MainWindow::showLoginDialog);
    connect(ui->btnRegister, &QPushButton::clicked, this, &MainWindow::showRegisterDialog);
    connect(ui->btnStart, &QPushButton::clicked, this, &MainWindow::showStudioPage);
}

MainWindow::~MainWindow()
{
    delete ui;
}

void MainWindow::showHomePage()
{
    ui->contentStack->setCurrentIndex(0);
    ui->btnHome->setStyleSheet(
        "QPushButton { font-size: 14px; color: #667eea; font-weight: 500; "
        "border: none; background: transparent; padding: 4px 0; }"
    );
    ui->btnStudio->setStyleSheet(
        "QPushButton { font-size: 14px; color: #6b7280; "
        "border: none; background: transparent; padding: 4px 0; }"
        "QPushButton:hover { color: #1f2937; }"
    );
}

void MainWindow::showStudioPage()
{
    ui->contentStack->setCurrentIndex(1);
    ui->btnHome->setStyleSheet(
        "QPushButton { font-size: 14px; color: #6b7280; "
        "border: none; background: transparent; padding: 4px 0; }"
        "QPushButton:hover { color: #1f2937; }"
    );
    ui->btnStudio->setStyleSheet(
        "QPushButton { font-size: 14px; color: #667eea; font-weight: 500; "
        "border: none; background: transparent; padding: 4px 0; }"
    );
}

void MainWindow::showAdminDialog()
{
    AdminDialog dlg(m_db, this);
    dlg.exec();
}

void MainWindow::showToast(const QString &msg)
{
    m_toast->setText(msg);
    m_toast->setStyleSheet(
        "QLabel {"
        "  background-color: #059669; color: white;"
        "  font-size: 14px; font-weight: 500;"
        "  padding: 8px 20px; border-radius: 6px;"
        "}"
    );
    m_toast->adjustSize();
    int x = (width() - m_toast->width()) / 2;
    int y = height() - 80;
    m_toast->move(x, y);
    m_toast->raise();
    m_toast->show();
    QTimer::singleShot(1000, m_toast, &QLabel::hide);
}

void MainWindow::showErrorToast(const QString &msg)
{
    m_toast->setText(msg);
    m_toast->setStyleSheet(
        "QLabel {"
        "  background-color: #dc2626; color: white;"
        "  font-size: 14px; font-weight: 500;"
        "  padding: 8px 20px; border-radius: 6px;"
        "}"
    );
    m_toast->adjustSize();
    int x = (width() - m_toast->width()) / 2;
    int y = height() - 80;
    m_toast->move(x, y);
    m_toast->raise();
    m_toast->show();
    QTimer::singleShot(1000, m_toast, &QLabel::hide);
}

void MainWindow::setupStudioPage()
{
    auto *outerLayout = qobject_cast<QVBoxLayout*>(ui->studioPage->layout());
    if (!outerLayout) {
        outerLayout = new QVBoxLayout(ui->studioPage);
    }
    outerLayout->setContentsMargins(24, 24, 24, 24);
    outerLayout->setSpacing(16);

    // ====== 标题 ======
    auto *titleRow = new QWidget();
    titleRow->setStyleSheet("background: transparent;");
    auto *titleL = new QHBoxLayout(titleRow);
    titleL->setContentsMargins(0, 0, 0, 0);
    auto *title = new QLabel("创作工作室");
    title->setStyleSheet("font-size: 20px; font-weight: 700; color: #1f2937;");
    titleL->addWidget(title);
    titleL->addStretch();
    outerLayout->addWidget(titleRow);

    // ====== 视频生成区（主体 - 占全部剩余空间） ======
    m_studioPreview = new QWidget();
    m_studioPreview->setStyleSheet(
        "background: qlineargradient(x1:0,y1:0,x2:0,y2:1,"
        "  stop:0 #1a1a2e, stop:1 #16213e);"
        "  border-radius: 12px;"
    );
    m_studioPreview->setMinimumHeight(200);

    m_studioPlayBtn = new QPushButton("▶");
    m_studioPlayBtn->setFixedSize(60, 60);
    m_studioPlayBtn->setStyleSheet(
        "QPushButton { background-color: rgba(255,255,255,0.12);"
        "  border: 2px solid rgba(255,255,255,0.25);"
        "  border-radius: 30px; font-size: 22px; color: white; }"
        "QPushButton:hover { background-color: rgba(255,255,255,0.22);"
        "  border: 2px solid rgba(255,255,255,0.4); }"
        "QPushButton:disabled { color: rgba(255,255,255,0.2); border-color: rgba(255,255,255,0.1); }"
    );
    m_studioPlayBtn->setCursor(Qt::PointingHandCursor);
    m_studioPlayBtn->setEnabled(false);

    auto *previewL = new QVBoxLayout(m_studioPreview);
    previewL->setAlignment(Qt::AlignCenter);
    previewL->addWidget(m_studioPlayBtn, 0, Qt::AlignCenter);

    m_studioHint = new QLabel("输入文案、上传素材后点击「生成视频」");
    m_studioHint->setStyleSheet("font-size: 12px; color: rgba(255,255,255,0.4); background: transparent;");
    m_studioHint->setAlignment(Qt::AlignCenter);
    previewL->addSpacing(10);
    previewL->addWidget(m_studioHint);

    outerLayout->addWidget(m_studioPreview, 1);

    // ====== 底部控制面板（固定高度区域） ======
    auto *bottomPanel = new QWidget();
    bottomPanel->setStyleSheet("background: transparent;");
    auto *panelL = new QVBoxLayout(bottomPanel);
    panelL->setContentsMargins(0, 0, 0, 0);
    panelL->setSpacing(10);

    // --- 进度条 ---
    m_studioProgress = new QProgressBar();
    m_studioProgress->setFixedHeight(6);
    m_studioProgress->setRange(0, 100);
    m_studioProgress->setValue(0);
    m_studioProgress->setTextVisible(false);
    m_studioProgress->setStyleSheet(
        "QProgressBar { background-color: #e5e7eb; border: none; border-radius: 3px; }"
        "QProgressBar::chunk { background: qlineargradient(x1:0,y1:0,x2:1,y2:0,"
        "  stop:0 #667eea, stop:1 #764ba2);"
        "  border-radius: 3px; }"
    );
    panelL->addWidget(m_studioProgress);

    // --- 文案输入 ---
    auto *inputLabel = new QLabel("文案");
    inputLabel->setStyleSheet("font-size: 14px; font-weight: 600; color: #374151;");

    m_studioTextEdit = new QTextEdit();
    m_studioTextEdit->setPlaceholderText("请输入视频文案内容...");
    m_studioTextEdit->setFixedHeight(60);
    m_studioTextEdit->setStyleSheet(
        "QTextEdit { font-size: 13px; padding: 10px;"
        "  background-color: #f9fafb; color: #1f2937;"
        "  border: 1px solid #e5e7eb; border-radius: 8px; }"
        "QTextEdit:focus { border-color: #667eea; }"
        "QTextEdit:disabled { background-color: #f3f4f6; color: #9ca3af; }"
    );

    panelL->addWidget(inputLabel);
    panelL->addWidget(m_studioTextEdit);

    // --- 底部操作栏：上传 + 生成 + 导出 ---
    auto *bottomRow = new QWidget();
    bottomRow->setStyleSheet("background: transparent;");
    auto *bottomL = new QHBoxLayout(bottomRow);
    bottomL->setContentsMargins(0, 0, 0, 0);
    bottomL->setSpacing(12);

    // --- 选择音色（上拉菜单） ---
    auto *voiceBtn = new QPushButton("  \xF0\x9F\x8E\xB5  选择音色  ");
    voiceBtn->setFixedHeight(44);
    voiceBtn->setStyleSheet(
        "QPushButton { font-size: 13px; color: #374151;"
        "  background-color: #f9fafb; border: 2px dashed #d1d5db;"
        "  border-radius: 8px; padding: 0 12px; }"
        "QPushButton:hover { border-color: #667eea; color: #667eea;"
        "  background-color: #f0f4ff; }"
    );
    voiceBtn->setCursor(Qt::PointingHandCursor);

    auto *voiceMenu = new QMenu(voiceBtn);
    voiceMenu->setStyleSheet(
        "QMenu { background: #ffffff; border: 1px solid #e5e7eb;"
        "  border-radius: 8px; padding: 6px; }"
        "QMenu::item { padding: 8px 24px; font-size: 13px;"
        "  color: #1f2937; border-radius: 4px; }"
        "QMenu::item:hover { background: #f3f4f6; color: #667eea; }"
    );
    voiceMenu->addAction("默认男声");
    voiceMenu->addAction("默认女声");
    voiceMenu->addAction("自定义克隆");

    auto *voiceLabel = new QLabel("当前：默认女声");
    voiceLabel->setStyleSheet("font-size: 11px; color: #9ca3af; background: transparent;");

    connect(voiceBtn, &QPushButton::clicked, this, [voiceBtn, voiceMenu]() {
        int menuH = voiceMenu->sizeHint().height();
        QPoint btnTopLeft = voiceBtn->mapToGlobal(QPoint(0, 0));
        int spaceAbove = btnTopLeft.y();
        int spaceBelow = QGuiApplication::primaryScreen()->availableGeometry().bottom() - (btnTopLeft.y() + voiceBtn->height());
        int menuY;
        if (spaceAbove >= menuH + 4) {
            menuY = btnTopLeft.y() - menuH - 4;
        } else if (spaceBelow >= menuH + 4) {
            menuY = btnTopLeft.y() + voiceBtn->height() + 4;
        } else {
            menuY = std::max(4, std::min(spaceAbove, 0));
        }
        voiceMenu->popup(QPoint(btnTopLeft.x(), menuY));
    });

    for (auto *action : voiceMenu->actions()) {
        connect(action, &QAction::triggered, this, [this, action, voiceLabel]() {
            m_selectedVoiceId = action->text();
            voiceLabel->setText("当前：" + m_selectedVoiceId);
        });
    }

    // --- 上传音频（用于声音克隆） ---
    auto *uploadAudioBtn = new QPushButton("  \xF0\x9F\x8E\xA4  上传音频  ");
    uploadAudioBtn->setFixedHeight(44);
    uploadAudioBtn->setStyleSheet(
        "QPushButton { font-size: 13px; color: #374151;"
        "  background-color: #f9fafb; border: 2px dashed #d1d5db;"
        "  border-radius: 8px; padding: 0 12px; }"
        "QPushButton:hover { border-color: #667eea; color: #667eea;"
        "  background-color: #f0f4ff; }"
    );
    uploadAudioBtn->setCursor(Qt::PointingHandCursor);

    auto *uploadAudioLabel = new QLabel("可选：用于克隆声音");
    uploadAudioLabel->setStyleSheet("font-size: 11px; color: #9ca3af; background: transparent;");

    // --- 上传视频（用于参考形象） ---
    auto *uploadVideoBtn = new QPushButton("  \xF0\x9F\x8E\xAC  上传视频  ");
    uploadVideoBtn->setFixedHeight(44);
    uploadVideoBtn->setStyleSheet(
        "QPushButton { font-size: 13px; color: #374151;"
        "  background-color: #f9fafb; border: 2px dashed #d1d5db;"
        "  border-radius: 8px; padding: 0 12px; }"
        "QPushButton:hover { border-color: #667eea; color: #667eea;"
        "  background-color: #f0f4ff; }"
    );
    uploadVideoBtn->setCursor(Qt::PointingHandCursor);

    auto *uploadVideoLabel = new QLabel("可选：用于参考形象");
    uploadVideoLabel->setStyleSheet("font-size: 11px; color: #9ca3af; background: transparent;");

    // 分组
    auto *voiceGroup = new QWidget();
    voiceGroup->setStyleSheet("background: transparent;");
    auto *voiceGroupL = new QVBoxLayout(voiceGroup);
    voiceGroupL->setContentsMargins(0, 0, 0, 0);
    voiceGroupL->setSpacing(2);
    voiceGroupL->addWidget(voiceBtn);
    voiceGroupL->addWidget(voiceLabel);

    auto *audioGroup = new QWidget();
    audioGroup->setStyleSheet("background: transparent;");
    auto *audioGroupL = new QVBoxLayout(audioGroup);
    audioGroupL->setContentsMargins(0, 0, 0, 0);
    audioGroupL->setSpacing(2);
    audioGroupL->addWidget(uploadAudioBtn);
    audioGroupL->addWidget(uploadAudioLabel);

    auto *videoGroup = new QWidget();
    videoGroup->setStyleSheet("background: transparent;");
    auto *videoGroupL = new QVBoxLayout(videoGroup);
    videoGroupL->setContentsMargins(0, 0, 0, 0);
    videoGroupL->setSpacing(2);
    videoGroupL->addWidget(uploadVideoBtn);
    videoGroupL->addWidget(uploadVideoLabel);

    // --- 语速调节（length_scale 系数：越大语速越慢、视频越长） ---
    auto *speedGroup = new QWidget();
    speedGroup->setStyleSheet("background: transparent;");
    auto *speedGroupL = new QVBoxLayout(speedGroup);
    speedGroupL->setContentsMargins(0, 0, 0, 0);
    speedGroupL->setSpacing(2);

    m_speedSlider = new QSlider(Qt::Horizontal);
    m_speedSlider->setRange(70, 150);   // 0.7x ~ 1.5x
    m_speedSlider->setValue(100);       // 默认 1.0x
    m_speedSlider->setFixedWidth(110);
    m_speedSlider->setCursor(Qt::PointingHandCursor);
    m_speedSlider->setToolTip("语速系数：越大越慢，视频越长");

    auto *speedLabel = new QLabel("语速 1.0x");
    speedLabel->setStyleSheet("font-size: 11px; color: #9ca3af; background: transparent;");

    speedGroupL->addWidget(speedLabel);
    speedGroupL->addWidget(m_speedSlider);

    connect(m_speedSlider, &QSlider::valueChanged, this, [speedLabel](int value) {
        speedLabel->setText(QString("语速 %1x").arg(value / 100.0, 0, 'f', 1));
    });

    // --- 生成按钮 ---
    m_studioGenerateBtn = new QPushButton("生成视频");
    m_studioGenerateBtn->setFixedSize(130, 44);
    m_studioGenerateBtn->setStyleSheet(
        "QPushButton { font-size: 14px; color: white;"
        "  background: qlineargradient(x1:0,y1:0,x2:1,y2:0,"
        "    stop:0 #f97316, stop:1 #ea580c);"
        "  border: none; border-radius: 10px; font-weight: 600; }"
        "QPushButton:hover {"
        "  background: qlineargradient(x1:0,y1:0,x2:1,y2:0,"
        "    stop:0 #ea580c, stop:1 #dc4400); }"
        "QPushButton:pressed { background: #dc4400; }"
        "QPushButton:disabled { background: #d1d5db; color: #9ca3af; }"
    );
    m_studioGenerateBtn->setCursor(Qt::PointingHandCursor);
    connect(m_studioGenerateBtn, &QPushButton::clicked, this, &MainWindow::onGenerateVideo);

    // --- 导出按钮 ---
    auto *btnExport = new QPushButton("导出");
    btnExport->setFixedSize(100, 44);
    btnExport->setStyleSheet(
        "QPushButton { font-size: 14px; color: #374151;"
        "  background-color: #ffffff; border: 1px solid #d1d5db;"
        "  border-radius: 10px; font-weight: 500; }"
        "QPushButton:hover { background-color: #f9fafb; border-color: #9ca3af; }"
        "QPushButton:pressed { background-color: #f3f4f6; }"
    );
    btnExport->setCursor(Qt::PointingHandCursor);

    // --- 组装操作栏 ---
    bottomL->addWidget(voiceGroup);
    bottomL->addWidget(audioGroup);
    bottomL->addWidget(videoGroup);
    bottomL->addWidget(speedGroup);
    bottomL->addStretch();
    bottomL->addWidget(m_studioGenerateBtn);
    bottomL->addSpacing(8);
    bottomL->addWidget(btnExport);

    panelL->addWidget(bottomRow);

    // 添加到主布局
    outerLayout->addWidget(bottomPanel);

    // ====== 文件选择逻辑 ======
    connect(uploadAudioBtn, &QPushButton::clicked, this, [this, uploadAudioLabel]() {
        QString path = QFileDialog::getOpenFileName(nullptr, "选择克隆音频",
            QString(), "音频文件 (*.wav *.mp3 *.m4a *.flac);;所有文件 (*.*)");
        if (!path.isEmpty()) {
            m_selectedAudioPath = path;
            QFileInfo fi(path);
            uploadAudioLabel->setText(fi.fileName());
        }
    });

    connect(uploadVideoBtn, &QPushButton::clicked, this, [this, uploadVideoLabel]() {
        QString path = QFileDialog::getOpenFileName(nullptr, "选择参考视频",
            QString(), "视频文件 (*.mp4 *.avi *.mov *.mkv);;所有文件 (*.*)");
        if (!path.isEmpty()) {
            m_selectedVideoPath = path;
            QFileInfo fi(path);
            uploadVideoLabel->setText(fi.fileName());
        }
    });

    // ====== 导出逻辑 ======
    connect(btnExport, &QPushButton::clicked, this, [this]() {
        QString path = QFileDialog::getSaveFileName(nullptr, "导出视频",
            QString(), "视频文件 (*.mp4 *.avi *.mov);;所有文件 (*)");
        if (!path.isEmpty()) {
            showToast("导出成功");
        }
    });
}

void MainWindow::showLoginDialog()
{
    LoginDialog dlg(m_db, this);
    if (dlg.exec() == QDialog::Accepted) {
        setLoggedInUser(dlg.loggedInUser());
    }
}

void MainWindow::setLoggedInUser(const QString &username)
{
    m_loggedInUser = username;
    // 替换登录/注册按钮为用户头像
    ui->m_userAvatar->setText(username.left(1).toUpper());
    ui->m_userAvatar->show();
    ui->m_userAvatar->setFixedSize(34, 34);

    // 隐藏登录/注册按钮
    auto btns = ui->m_authBox->findChildren<QPushButton*>();
    for (auto *btn : btns) {
        if (btn != ui->m_userAvatar) btn->hide();
    }

    // 创建下拉菜单
    auto *menu = new QMenu(this);
    menu->setStyleSheet(
        "QMenu { background: #ffffff; border: 1px solid #e5e7eb;"
        "  border-radius: 8px; padding: 4px; }"
        "QMenu::item { font-size: 13px; color: #374151; padding: 8px 16px;"
        "  border-radius: 4px; margin: 2px 0; }"
        "QMenu::item:hover { background: #f3f4f6; color: #1f2937; }"
        "QMenu::separator { height: 1px; background: #e5e7eb; margin: 4px 8px; }"
    );

    auto *userAction = menu->addAction(username);
    userAction->setEnabled(false);

    menu->addSeparator();

    auto *logoutAction = menu->addAction("退出登录");
    connect(logoutAction, &QAction::triggered, this, [this]() {
        m_loggedInUser.clear();
        ui->m_userAvatar->hide();
        // 恢复显示登录/注册按钮
        auto btns = ui->m_authBox->findChildren<QPushButton*>();
        for (auto *btn : btns) {
            if (btn != ui->m_userAvatar) btn->show();
        }
        // 切换到首页
        showHomePage();
    });

    // 点击头像弹出菜单
    ui->m_userAvatar->setCursor(Qt::PointingHandCursor);
    disconnect(ui->m_userAvatar, &QPushButton::clicked, nullptr, nullptr);
    connect(ui->m_userAvatar, &QPushButton::clicked, this, [this, menu]() {
        menu->setMinimumWidth(140);
        menu->adjustSize();

        QPoint btnTopRight = ui->m_userAvatar->mapToGlobal(QPoint(ui->m_userAvatar->width(), 0));
        QRect screenRect = QGuiApplication::primaryScreen()->geometry();
        int menuW = qMin(menu->sizeHint().width(), 220);

        int menuX = btnTopRight.x() - menuW;
        int menuY = btnTopRight.y() + ui->m_userAvatar->height() + 4;

        if (menuX < screenRect.left() + 4)
            menuX = screenRect.left() + 4;
        if (menuX + menuW > screenRect.right() - 4)
            menuX = screenRect.right() - menuW - 4;
        if (menuY + menu->sizeHint().height() > screenRect.bottom() - 4)
            menuY = btnTopRight.y() - menu->sizeHint().height() - 4;
        if (menuY < screenRect.top() + 4)
            menuY = screenRect.top() + 4;

        menu->popup(QPoint(menuX, menuY));
    });
}

void MainWindow::showRegisterDialog()
{
    RegisterDialog dlg(m_db, this);
    dlg.exec();
}

void MainWindow::onGenerateVideo()
{
    QString text = m_studioTextEdit->toPlainText().trimmed();
    if (text.isEmpty()) {
        showErrorToast("请输入视频文案");
        return;
    }

    QString voiceId = m_selectedVoiceId;
    QString voiceRefAudio = (voiceId == "自定义克隆") ? m_selectedAudioPath : QString();
    QString avatarId = m_selectedVideoPath.isEmpty() ? "默认形象" : m_selectedVideoPath;

    m_studioGenerateBtn->setEnabled(false);
    m_studioTextEdit->setEnabled(false);
    m_studioProgress->setValue(0);
    m_studioHint->setText("正在提交生成任务...");
    m_studioHint->setStyleSheet("font-size: 12px; color: rgba(255,255,255,0.7); background: transparent;");
    m_currentVideoUrl.clear();
    m_studioPlayBtn->setEnabled(false);

    double speed = m_speedSlider ? m_speedSlider->value() / 100.0 : 1.0;
    m_aiService->generateVideo(text, avatarId, voiceId, voiceRefAudio, speed);
}

void MainWindow::onVideoGenerated(const QString &videoUrl, const QString &taskId)
{
    m_currentTaskId = taskId;
    if (!taskId.isEmpty()) {
        m_pollTimer->start();
    }
}

void MainWindow::onTaskStatusUpdated(const QString &status, int progress,
                                     const QString &videoUrl, const QString &message)
{
    m_studioProgress->setValue(progress);
    m_studioHint->setText(message);

    if (status == "completed") {
        m_pollTimer->stop();
        m_studioGenerateBtn->setEnabled(true);
        m_studioTextEdit->setEnabled(true);
        m_currentVideoUrl = videoUrl;
        m_studioPlayBtn->setEnabled(true);
        updatePreview(videoUrl);
        showToast("视频生成完成");
    } else if (status == "failed") {
        m_pollTimer->stop();
        m_studioGenerateBtn->setEnabled(true);
        m_studioTextEdit->setEnabled(true);
        m_studioHint->setStyleSheet("font-size: 12px; color: #fca5a5; background: transparent;");
        showErrorToast(message);
    }
}

void MainWindow::onErrorOccurred(const QString &error)
{
    m_pollTimer->stop();
    m_studioGenerateBtn->setEnabled(true);
    m_studioTextEdit->setEnabled(true);
    m_studioHint->setText(error);
    m_studioHint->setStyleSheet("font-size: 12px; color: #fca5a5; background: transparent;");
    showErrorToast(error);
}

void MainWindow::pollTaskStatus()
{
    if (!m_currentTaskId.isEmpty()) {
        m_aiService->queryTaskStatus(m_currentTaskId);
    }
}

void MainWindow::updatePreview(const QString &videoUrl)
{
    if (videoUrl.isEmpty()) {
        return;
    }

    // 后端返回的是已编码的 file:/// URL，这里统一用 QUrl 解码为本地路径
    QUrl url(videoUrl);
    QString localPath = url.toLocalFile();
    if (localPath.isEmpty()) {
        localPath = videoUrl;
    }

    QFileInfo fi(localPath);
    m_studioHint->setText(QString("视频已生成: %1").arg(fi.fileName()));
    m_studioHint->setStyleSheet("font-size: 12px; color: rgba(255,255,255,0.7); background: transparent;");

    // 点击播放按钮用系统默认播放器打开视频
    disconnect(m_studioPlayBtn, &QPushButton::clicked, nullptr, nullptr);
    connect(m_studioPlayBtn, &QPushButton::clicked, this, [localPath]() {
        QDesktopServices::openUrl(QUrl::fromLocalFile(localPath));
    });
}


