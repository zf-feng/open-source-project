#ifndef ADMINDIALOG_H
#define ADMINDIALOG_H

#include <QDialog>

class DbHelper;

namespace Ui {
class AdminDialog;
}

class AdminDialog : public QDialog
{
    Q_OBJECT
public:
    explicit AdminDialog(DbHelper *db, QWidget *parent = nullptr);

private:
    void loadUsers();

    DbHelper *m_db;
    Ui::AdminDialog *ui;
};

#endif // ADMINDIALOG_H
