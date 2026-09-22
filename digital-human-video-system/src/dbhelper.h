#ifndef DBHELPER_H
#define DBHELPER_H

#include <QObject>
#include <QSqlDatabase>
#include <QVector>

struct UserInfo {
    QString name;
    QString password;
};

class DbHelper : public QObject
{
    Q_OBJECT
public:
    explicit DbHelper(QObject *parent = nullptr);
    ~DbHelper();

    bool open();
    void close();
    bool isConnected() const;
    QString lastError() const { return m_lastError; }

    bool login(const QString &username, const QString &password);
    bool registerUser(const QString &username, const QString &password, QString &errorMsg);
    bool isAdmin(const QString &username);
    QVector<UserInfo> getAllUsers();
    bool updateUser(const QString &oldName, const QString &newName, const QString &newPassword, QString &errorMsg);

private:
    QSqlDatabase m_db;
    QString m_lastError;
    QString m_dbPath = "D:/QT Code/QT1/data/users.db";
};

#endif // DBHELPER_H
