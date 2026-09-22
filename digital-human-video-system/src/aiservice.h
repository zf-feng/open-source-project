#ifndef AISERVICE_H
#define AISERVICE_H

#include <QObject>
#include <QString>
#include <QNetworkAccessManager>
#include <QNetworkReply>

class AiService : public QObject
{
    Q_OBJECT

public:
    explicit AiService(QObject *parent = nullptr);

    // AI 文本生成
    void generateText(const QString &prompt);

    // 数字人视频生成
    void generateVideo(const QString &text, const QString &avatarId, const QString &voiceId, const QString &voiceRefAudio = QString(), double speed = 1.0);

    // 查询任务状态
    void queryTaskStatus(const QString &taskId);

signals:
    void textGenerated(const QString &text);
    void videoGenerated(const QString &videoUrl, const QString &taskId);
    void taskStatusUpdated(const QString &status, int progress, const QString &videoUrl, const QString &message);
    void errorOccurred(const QString &error);

private:
    QNetworkAccessManager *networkManager;

    // API 配置
    QString apiBaseUrl = "http://localhost:8000/api";
    QString apiKey = "";
};

#endif // AISERVICE_H
