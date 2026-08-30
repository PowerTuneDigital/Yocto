SUMMARY = "PowerTune Recovery is a fallback launcher for PowerTune"
SECTION = "libs"
HOMEPAGE = "https://github.com/PowerTuneDigital/PowerTuneDigitalRecovery"

LICENSE = "CLOSED"

DEPENDS = "qtbase qtdeclarative"
RDEPENDS:${PN} += " qtvirtualkeyboard qtsvg qtsvg-plugins qtdeclarative-qmlplugins qtbase-qmlplugins qtbase-plugins bash"

inherit qmake5

SRCREV = "${AUTOREV}"
SRC_URI = "git://github.com/PowerTuneDigital/PowerTuneDigitalRecovery;protocol=https;branch=main"

S = "${WORKDIR}/git"

# Mirror this project's own updateRecovery.sh convention: the buildable
# source lives in /home/pi/Recoverysrc, and /home/pi/Recovery is the
# out-of-source shadow build dir the on-device "qmake /home/pi/Recoverysrc
# && make" self-update flow runs in. Ship a prebuilt binary there too, so
# a stock image doesn't need to compile it on first boot.
do_install() {
    install -d ${D}/home/pi/Recovery
    install -m 0755 -p ${B}/Recovery ${D}/home/pi/Recovery/Recovery

    install -d ${D}/home/pi/Recoverysrc
    install -m 0644 ${S}/Recovery.pro ${S}/PTrecovery.cpp ${S}/PTrecovery.h \
        ${S}/main.cpp ${S}/main.qml ${S}/WifiCountryList.qml ${S}/qml.qrc \
        ${S}/Logo.png ${D}/home/pi/Recoverysrc/
    install -m 0755 -p ${S}/updateRecovery.sh ${S}/updatepi4launchscript.sh \
        ${D}/home/pi/Recoverysrc/
}

FILES:${PN} += "/home/pi/Recovery /home/pi/Recoverysrc"
