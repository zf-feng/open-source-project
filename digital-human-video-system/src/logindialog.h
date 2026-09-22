#ifndef LOGINDIALOG_H
#define LOGINDIALOG_H

#include <QDialog>

class DbHelper;

namespace Ui {
class LoginDialog;
}

class LoginDialog : public QDialog
{
    Q_OBJECT
public:
    explicit LoginDialog(DbHelper *db, QWidget *parent = nullptr);
    ~LoginDialog();
    QString loggedInUser() const { return m_loggedInUser; }

private:
    DbHelper *m_db;
    QString m_loggedInUser;
    Ui::LoginDialog *ui;
};

#endif // LOGINDIALOG_H
