#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QLabel>
#include <QDialog>
#include <QStackedWidget>
#include <QTextEdit>
#include <QProgressBar>
#include <QPushButton>
#include <QTimer>
#include <QSlider>

#include "aiservice.h"

class DbHelper;

namespace Ui {
class MainWindow;
}

class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    MainWindow(QWidget *parent = nullptr);
    ~MainWindow();
    void showAdminDialog();
    void showToast(const QString &msg);
    void showErrorToast(const QString &msg);
    void setLoggedInUser(const QString &username);

private slots:
    void showLoginDialog();
    void showRegisterDialog();
    void showStudioPage();
    void showHomePage();

    void onGenerateVideo();
    void onVideoGenerated(const QString &videoUrl, const QString &taskId);
    void onTaskStatusUpdated(const QString &status, int progress, const QString &videoUrl, const QString &message);
    void onErrorOccurred(const QString &error);
    void pollTaskStatus();

private:
    void setupStudioPage();
    void updatePreview(const QString &videoUrl);

    Ui::MainWindow *ui;
    QLabel *m_toast;
    QString m_loggedInUser;
    DbHelper *m_db;

    AiService *m_aiService = nullptr;

    // 创作页控件
    QTextEdit *m_studioTextEdit = nullptr;
    QProgressBar *m_studioProgress = nullptr;
    QLabel *m_studioHint = nullptr;
    QWidget *m_studioPreview = nullptr;
    QPushButton *m_studioGenerateBtn = nullptr;
    QPushButton *m_studioPlayBtn = nullptr;
    QSlider *m_speedSlider = nullptr;

    QString m_currentTaskId;
    QString m_currentVideoUrl;
    QString m_selectedVoiceId = "默认女声";
    QString m_selectedAudioPath;
    QString m_selectedVideoPath;
    QTimer *m_pollTimer = nullptr;
};

#endif // MAINWINDOW_H
