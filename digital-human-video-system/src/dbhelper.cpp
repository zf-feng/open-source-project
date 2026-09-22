#include "dbhelper.h"
#include <QSqlQuery>
#include <QSqlError>
#include <QSqlDriver>
#include <QDebug>
#include <QMessageBox>

DbHelper::DbHelper(QObject *parent)
    : QObject(parent)
{
}

DbHelper::~DbHelper()
{
    close();
}

bool DbHelper::open()
{
    if (m_db.isOpen()) return true;

    const QString connName = "yonghu_conn";

    // 清理旧的残留连接（必须先 reset m_db）
    if (QSqlDatabase::contains(connName)) {
        m_db = QSqlDatabase();  // 先释放引用
        QSqlDatabase::removeDatabase(connName);
    }

    // 使用 SQLite 本地数据库
    m_db = QSqlDatabase::addDatabase("QSQLITE", connName);
    m_db.setDatabaseName(m_dbPath);

    if (!m_db.open()) {
        m_lastError = "数据库打开失败: " + m_db.lastError().text();
        qWarning() << "数据库连接失败:" << m_lastError;
        return false;
    }

    qDebug() << "数据库连接成功 (SQLite):" << m_dbPath;

    // 自动创建用户表
    QSqlQuery query(m_db);
    if (!query.exec("CREATE TABLE IF NOT EXISTS yonghu ("
                    "name TEXT PRIMARY KEY NOT NULL, "
                    "password TEXT NOT NULL)")) {
        qWarning() << "创建表失败:" << query.lastError().text();
    }

    return true;
}

void DbHelper::close()
{
    if (m_db.isOpen()) {
        m_db.close();
    }
    QString connName = m_db.connectionName();
    if (!connName.isEmpty()) {
        m_db = QSqlDatabase();
        QSqlDatabase::removeDatabase(connName);
    }
}

bool DbHelper::isConnected() const
{
    return m_db.isOpen();
}

bool DbHelper::login(const QString &username, const QString &password)
{
    if (!m_db.isOpen()) {
        if (!open()) {
            m_lastError = "数据库未连接";
            return false;
        }
    }

    QSqlQuery query(m_db);

    query.prepare("SELECT name FROM yonghu WHERE name = :username AND password = :password");
    query.bindValue(":username", username);
    query.bindValue(":password", password);
    if (!query.exec()) {
        m_lastError = "查询失败: " + query.lastError().text();
        return false;
    }

    if (query.next()) {
        return true;
    }

    m_lastError = "密码错误";
    return false;
}

bool DbHelper::registerUser(const QString &username, const QString &password, QString &errorMsg)
{
    if (!m_db.isOpen()) {
        if (!open()) {
            errorMsg = "数据库未连接";
            return false;
        }
    }

    QSqlQuery query(m_db);

    query.prepare("SELECT name FROM yonghu WHERE name = :username");
    query.bindValue(":username", username);
    if (!query.exec()) {
        errorMsg = "查询失败: " + query.lastError().text();
        return false;
    }
    if (query.next()) {
        errorMsg = "用户名已存在";
        return false;
    }

    query.prepare("INSERT INTO yonghu (name, password) VALUES (:username, :password)");
    query.bindValue(":username", username);
    query.bindValue(":password", password);

    if (!query.exec()) {
        errorMsg = "注册失败: " + query.lastError().text();
        return false;
    }

    return true;
}

bool DbHelper::isAdmin(const QString &username)
{
    Q_UNUSED(username);
    return false;
}

QVector<UserInfo> DbHelper::getAllUsers()
{
    QVector<UserInfo> users;

    if (!m_db.isOpen()) {
        if (!open()) {
            m_lastError = "数据库未连接";
            return users;
        }
    }

    QSqlQuery query(m_db);
    if (!query.exec("SELECT name, password FROM yonghu ORDER BY name")) {
        m_lastError = "查询失败: " + query.lastError().text();
        return users;
    }

    while (query.next()) {
        UserInfo u;
        u.name = query.value(0).toString();
        u.password = query.value(1).toString();
        users.append(u);
    }
    return users;
}

bool DbHelper::updateUser(const QString &oldName, const QString &newName, const QString &newPassword, QString &errorMsg)
{
    if (!m_db.isOpen()) {
        if (!open()) {
            errorMsg = "数据库未连接";
            return false;
        }
    }

    QSqlQuery query(m_db);

    if (oldName != newName) {
        query.prepare("SELECT name FROM yonghu WHERE name = :name");
        query.bindValue(":name", newName);
        if (!query.exec()) {
            errorMsg = "查询失败: " + query.lastError().text();
            return false;
        }
        if (query.next()) {
            errorMsg = "用户名已存在";
            return false;
        }
    }

    query.prepare("UPDATE yonghu SET name = :newName, password = :password WHERE name = :oldName");
    query.bindValue(":newName", newName);
    query.bindValue(":password", newPassword);
    query.bindValue(":oldName", oldName);

    if (!query.exec()) {
        errorMsg = "更新失败: " + query.lastError().text();
        return false;
    }
    return true;
}
