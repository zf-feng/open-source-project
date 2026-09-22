QT       += core gui widgets network sql

greaterThan(QT_MAJOR_VERSION, 4): QT += widgets

CONFIG += c++17

TARGET = DigitalHuman
TEMPLATE = app

SOURCES += \
    src/main.cpp \
    src/mainwindow.cpp \
    src/aiservice.cpp \
    src/dbhelper.cpp \
    src/logindialog.cpp \
    src/registerdialog.cpp \
    src/admindialog.cpp

HEADERS += \
    src/mainwindow.h \
    src/aiservice.h \
    src/dbhelper.h \
    src/logindialog.h \
    src/registerdialog.h \
    src/admindialog.h

FORMS += \
    forms/mainwindow.ui \
    forms/logindialog.ui \
    forms/registerdialog.ui \
    forms/admindialog.ui

RESOURCES += resources.qrc

# 输出目录 - 保持根目录整洁
DESTDIR = $$PWD/bin
OBJECTS_DIR = build/obj
MOC_DIR = build/moc
RCC_DIR = build/rcc
UI_DIR = build/ui
INCLUDEPATH += build/ui

# 在 Windows 上不显示控制台窗口
win32: CONFIG -= console
win32: CONFIG += windows
