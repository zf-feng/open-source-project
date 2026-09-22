#include "mainwindow.h"
#include "ui_mainwindow.h"
#include <QCoreApplication>
#include <QDir>
#include <QUrl>
#include <QJsonParseError>
#include <QDateTime>

MainWindow::MainWindow(QWidget *parent)
    : QMainWindow(parent)
    , ui(new Ui::MainWindow)
    , scene(nullptr)
    , videoItem(nullptr)
    , player(nullptr)
    , m_currentProgress(0)
    , m_progressTimer(nullptr)
{
    ui->setupUi(this);

    ui->comboBox_voice->clear();
    ui->comboBox_voice->addItem("默认男声");
    ui->comboBox_voice->addItem("默认女声");
    ui->comboBox_voice->addItem("自定义克隆");
    ui->comboBox_voice->setCurrentIndex(0);

    ui->progressBar_status->setValue(0);
    ui->progressBar_status->setRange(0, 100);

    setupVideoPlayer();

    m_progressTimer = new QTimer(this);
    connect(m_progressTimer, &QTimer::timeout, this, &MainWindow::on_progress_update);

    appendLog("系统已就绪，请输入文本并点击生成视频");
}

MainWindow::~MainWindow()
{
    if (player) {
        player->stop();
    }
    delete ui;
}

void MainWindow::setupVideoPlayer()
{
    scene = new QGraphicsScene(this);
    videoItem = new QGraphicsVideoItem();

    qreal viewWidth = ui->videoGraphicsView->width();
    qreal viewHeight = ui->videoGraphicsView->height();
    if (viewWidth <= 0 || viewHeight <= 0) {
        viewWidth = 640;
        viewHeight = 480;
    }
    videoItem->setSize(QSizeF(viewWidth, viewHeight));

    scene->addItem(videoItem);
    ui->videoGraphicsView->setScene(scene);

    player = new QMediaPlayer(this);
    player->setVideoOutput(videoItem);

    connect(player, QOverload<QMediaPlayer::PlaybackState>::of(&QMediaPlayer::playbackStateChanged),
            this, &MainWindow::on_player_stateChanged);
}

void MainWindow::on_btn_generate_clicked()
{
    QString text = ui->textEdit_input->toPlainText().trimmed();
    if (text.isEmpty()) {
        showError("请输入要播报的文本内容");
        return;
    }

    QString voice = ui->comboBox_voice->currentText();
    if (voice == "自定义克隆" && m_refAudioPath.isEmpty()) {
        showError("选择自定义克隆音色时，必须上传参考音频");
        return;
    }

    updateProgress(0);
    m_currentProgress = 0;

    ui->btn_generate->setEnabled(false);
    ui->btn_generate->setText("生成中...");
    m_progressTimer->start(300);

    appendLog("开始生成数字人视频...");

    QString audioPath = callVoiceAPI(text, voice, m_refAudioPath);
    if (audioPath.isEmpty()) {
        showError("语音生成失败");
        ui->btn_generate->setEnabled(true);
        ui->btn_generate->setText("生成视频");
        m_progressTimer->stop();
        return;
    }
    appendLog("语音生成成功: " + audioPath);
    updateProgress(40);

    QString videoPath = callVideoAPI(audioPath, m_refVideoPath);
    if (videoPath.isEmpty()) {
        showError("视频生成失败");
        ui->btn_generate->setEnabled(true);
        ui->btn_generate->setText("生成视频");
        m_progressTimer->stop();
        return;
    }
    appendLog("视频生成成功: " + videoPath);
    updateProgress(90);

    player->setSource(QUrl::fromLocalFile(videoPath));
    player->play();
    updateProgress(100);

    ui->btn_generate->setEnabled(true);
    ui->btn_generate->setText("生成视频");
    m_progressTimer->stop();

    showSuccess("数字人视频生成完成");
}

void MainWindow::on_btn_upload_video_clicked()
{
    QString filePath = QFileDialog::getOpenFileName(
        this,
        "选择参考视频",
        QDir::homePath(),
        "视频文件 (*.mp4 *.avi *.mov *.mkv);;所有文件 (*)"
        );

    if (!filePath.isEmpty()) {
        m_refVideoPath = filePath;
        ui->btn_upload_video->setText(QFileInfo(filePath).fileName());
        appendLog("已上传参考视频: " + QFileInfo(filePath).fileName());
    }
}

void MainWindow::on_btn_upload_audio_clicked()
{
    QString filePath = QFileDialog::getOpenFileName(
        this,
        "选择参考音频",
        QDir::homePath(),
        "音频文件 (*.wav *.mp3 *.m4a *.flac);;所有文件 (*)"
        );

    if (!filePath.isEmpty()) {
        m_refAudioPath = filePath;
        ui->btn_upload_audio->setText(QFileInfo(filePath).fileName());
        appendLog("已上传参考音频: " + QFileInfo(filePath).fileName());

        if (ui->comboBox_voice->currentText() != "自定义克隆") {
            ui->comboBox_voice->setCurrentIndex(2);
        }
    }
}

void MainWindow::on_comboBox_voice_currentIndexChanged(int index)
{
    Q_UNUSED(index);
    QString voice = ui->comboBox_voice->currentText();
    if (voice == "自定义克隆" && m_refAudioPath.isEmpty()) {
        appendLog("提示: 请上传参考音频进行声音克隆");
    }
    appendLog("切换到: " + voice);
}

void MainWindow::on_player_stateChanged(QMediaPlayer::PlaybackState state)
{
    if (state == QMediaPlayer::PlayingState) {
        appendLog("视频开始播放");
    }
}

void MainWindow::on_progress_update()
{
    if (m_currentProgress < 90) {
        m_currentProgress += rand() % 5 + 1;
        if (m_currentProgress > 90) m_currentProgress = 90;
        updateProgress(m_currentProgress);
    }
}

// ============================================================
//  测试模式：不调用Python，直接返回假路径
//  等模型对接好后，替换成真实调用版本
// ============================================================

QString MainWindow::callVoiceAPI(const QString &text, const QString &voice, const QString &refAudio)
{
    Q_UNUSED(text);
    Q_UNUSED(voice);
    Q_UNUSED(refAudio);

    appendLog("【测试模式】语音生成API被调用（未对接真实模型）");
    return "./output/test_audio.wav";
}

QString MainWindow::callVideoAPI(const QString &audioPath, const QString &refVideo)
{
    Q_UNUSED(audioPath);
    Q_UNUSED(refVideo);

    appendLog("【测试模式】视频生成API被调用（未对接真实模型）");
    return "./output/test_video.mp4";
}

void MainWindow::updateProgress(int value)
{
    if (value > 100) value = 100;
    if (value < 0) value = 0;
    ui->progressBar_status->setValue(value);
}

void MainWindow::appendLog(const QString &msg)
{
    QString time = QDateTime::currentDateTime().toString("hh:mm:ss");
    qDebug() << "[" + time + "] " + msg;
    if (statusBar()) {
        statusBar()->showMessage(msg, 3000);
    }
}

void MainWindow::showError(const QString &msg)
{
    appendLog("错误: " + msg);
    QMessageBox::critical(this, "错误", msg);
}

void MainWindow::showSuccess(const QString &msg)
{
    appendLog("成功: " + msg);
    QMessageBox::information(this, "提示", msg);
}