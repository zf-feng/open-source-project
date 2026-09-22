#include "aiservice.h"
#include <QJsonDocument>
#include <QJsonObject>
#include <QNetworkRequest>

AiService::AiService(QObject *parent)
    : QObject(parent)
    , networkManager(new QNetworkAccessManager(this))
{
}

void AiService::generateText(const QString &prompt)
{
    QUrl url(apiBaseUrl + "/ai/generate-text");
    QNetworkRequest request(url);
    request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");

    if (!apiKey.isEmpty()) {
        request.setRawHeader("Authorization", ("Bearer " + apiKey).toUtf8());
    }

    QJsonObject body;
    body["prompt"] = prompt;
    body["model"] = "default";

    QNetworkReply *reply = networkManager->post(request, QJsonDocument(body).toJson());
    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        reply->deleteLater();
        if (reply->error() != QNetworkReply::NoError) {
            emit errorOccurred("API请求失败: " + reply->errorString());
            return;
        }
        QJsonDocument doc = QJsonDocument::fromJson(reply->readAll());
        QJsonObject data = doc.object()["data"].toObject();
        QString text = data["text"].toString();
        emit textGenerated(text);
    });
}

void AiService::generateVideo(const QString &text, const QString &avatarId, const QString &voiceId, const QString &voiceRefAudio, double speed)
{
    QUrl url(apiBaseUrl + "/ai/generate-video");
    QNetworkRequest request(url);
    request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");

    if (!apiKey.isEmpty()) {
        request.setRawHeader("Authorization", ("Bearer " + apiKey).toUtf8());
    }

    QJsonObject body;
    body["text"] = text;
    body["avatarId"] = avatarId;
    body["voiceId"] = voiceId;
    if (!voiceRefAudio.isEmpty()) {
        body["voiceRefAudio"] = voiceRefAudio;
    }
    body["speed"] = speed;

    QNetworkReply *reply = networkManager->post(request, QJsonDocument(body).toJson());
    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        reply->deleteLater();
        if (reply->error() != QNetworkReply::NoError) {
            emit errorOccurred("视频生成请求失败: " + reply->errorString());
            return;
        }
        QJsonDocument doc = QJsonDocument::fromJson(reply->readAll());
        QJsonObject data = doc.object()["data"].toObject();
        QString videoUrl = data["videoUrl"].toString();
        QString taskId = data["taskId"].toString();
        emit videoGenerated(videoUrl, taskId);
    });
}

void AiService::queryTaskStatus(const QString &taskId)
{
    QUrl url(apiBaseUrl + "/ai/task/" + taskId);
    QNetworkRequest request(url);

    if (!apiKey.isEmpty()) {
        request.setRawHeader("Authorization", ("Bearer " + apiKey).toUtf8());
    }

    QNetworkReply *reply = networkManager->get(request);
    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        reply->deleteLater();
        if (reply->error() != QNetworkReply::NoError) {
            emit errorOccurred("查询任务状态失败: " + reply->errorString());
            return;
        }
        QJsonDocument doc = QJsonDocument::fromJson(reply->readAll());
        QJsonObject data = doc.object()["data"].toObject();
        QString status = data["status"].toString();
        int progress = data["progress"].toInt();
        QString videoUrl = data["videoUrl"].toString();
        QString message = data["message"].toString();
        emit taskStatusUpdated(status, progress, videoUrl, message);
    });
}
