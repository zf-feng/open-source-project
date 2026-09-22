#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QGraphicsScene>
#include <QGraphicsVideoItem>
#include <QMediaPlayer>
#include <QProcess>
#include <QFileDialog>
#include <QMessageBox>
#include <QProgressBar>
#include <QDebug>
#include <QJsonDocument>
#include <QJsonObject>
#include <QFileInfo>
#include <QTimer>
#include <QStatusBar>
#include <QVBoxLayout>
#include <QWidget>

QT_BEGIN_NAMESPACE
namespace Ui {
class MainWindow;
}
QT_END_NAMESPACE

class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = nullptr);
    ~MainWindow() override;

private slots:
    void on_btn_generate_clicked();
    void on_btn_upload_video_clicked();
    void on_btn_upload_audio_clicked();
    void on_comboBox_voice_currentIndexChanged(int index);
    void on_player_stateChanged(QMediaPlayer::PlaybackState state);
    void on_progress_update();

private:
    Ui::MainWindow *ui;

    // 视频播放
    QGraphicsScene *scene;
    QGraphicsVideoItem *videoItem;
    QMediaPlayer *player;

    // 文件路径
    QString m_refVideoPath;
    QString m_refAudioPath;

    // 进度
    int m_currentProgress;
    QTimer *m_progressTimer;

    // 辅助函数
    void setupVideoPlayer();
    QString callVoiceAPI(const QString &text, const QString &voice, const QString &refAudio);
    QString callVideoAPI(const QString &audioPath, const QString &refVideo);
    void updateProgress(int value);
    void appendLog(const QString &msg);
    void showError(const QString &msg);
    void showSuccess(const QString &msg);
};

#endif // MAINWINDOW_H