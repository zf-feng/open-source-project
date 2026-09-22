#include "logindialog.h"
#include "ui_logindialog.h"
#include "dbhelper.h"
#include "mainwindow.h"
#include <QMessageBox>

LoginDialog::LoginDialog(DbHelper *db, QWidget *parent)
    : QDialog(parent), m_db(db)
{
    ui = new Ui::LoginDialog;
    ui->setupUi(this);

    setWindowTitle("登录");
    setFixedSize(380, 340);

    // 信号连接
    connect(ui->btnSubmit, &QPushButton::clicked, this, [this]() {
        if (!m_db || !m_db->isConnected()) {
            QMessageBox::warning(this, "数据库错误",
                "无法连接到数据库\n\n" + (m_db ? m_db->lastError() : "数据库未初始化"));
            return;
        }
        QString user = ui->editUser->text().trimmed();
        QString pwd = ui->editPwd->text();
        if (user.isEmpty() || pwd.isEmpty()) {
            auto *mw = qobject_cast<MainWindow*>(parentWidget());
            if (mw) mw->showErrorToast("请输入用户名和密码");
            return;
        }
        if (m_db->login(user, pwd)) {
            m_loggedInUser = user;
            auto *mw = qobject_cast<MainWindow*>(parentWidget());
            if (mw) mw->showToast("登录成功");
            accept();
        } else {
            auto *mw = qobject_cast<MainWindow*>(parentWidget());
            if (mw) mw->showErrorToast(m_db->lastError());
        }
    });

    connect(ui->btnAdmin, &QPushButton::clicked, this, [this]() {
        auto *mw = qobject_cast<MainWindow*>(parentWidget());
        if (mw) mw->showAdminDialog();
    });
}

LoginDialog::~LoginDialog()
{
    delete ui;
}
