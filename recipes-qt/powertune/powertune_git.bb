SUMMARY = "PowerTune is a Modern Gauge Display"
SECTION = "libs"
HOMEPAGE = "https://github.com/PowerTuneDigital/PowerTuneDigitalOfficial"

# GPLv3, except the daemons/ directory which is proprietary and
# confidential per the notice appended to LICENSE.
LICENSE = "GPLv3 & Proprietary"
LIC_FILES_CHKSUM = "file://LICENSE;md5=8363ae0c1160a2a2b3b14d95fd43bdb5"

DEPENDS = "qtbase qttools"
DEPENDS += " qtserialbus qtcharts qtlocation qtsensors qtmultimedia qtquickcontrols2 qtdeclarative qtgraphicaleffects qtsvg"
RDEPENDS:${PN} += " sudo qtvirtualkeyboard qtsvg-plugins qtxmlpatterns qtdeclarative-qmlplugins qtgraphicaleffects-qmlplugins qtquickcontrols-qmlplugins qtlocation-qmlplugins qtsensors-qmlplugins qtbase-qmlplugins qtbase-plugins libsocketcan powertune-recovery openssl-legacy"

inherit qmake5
inherit useradd

SRCREV = "${AUTOREV}"
SRC_URI = " \
    git://github.com/PowerTuneDigital/PowerTuneDigitalOfficial;protocol=https;branch=main \
    file://powertune-update.sh \
    file://startdaemon.sh \
    file://updatePowerTune.sh \
    "

S = "${WORKDIR}/git"

USERADD_PACKAGES = "${PN}"

# powertune
USERADD_PARAM:${PN} = "-d /home/pi -s /bin/bash -p '$6$u30tO9Iobu19Ak6p$40C6YgGQOhUNCgDx6bQMskQcrIlSzRugqENWCaqLXAOrjV2TKTFtRYWQPXPWOBjsRE/7xMMeagqK5fceZstO81' pi"

do_install:append() {
    install -d ${D}/home/pi
    install -d ${D}/opt/PowerTune
    install -m 0755 -p ${WORKDIR}/powertune-update.sh ${D}/home/pi/powertune-update.sh
    install -m 0755 -p ${WORKDIR}/startdaemon.sh ${D}/home/pi/startdaemon.sh
    install -m 0755 -p ${WORKDIR}/updatePowerTune.sh ${D}/home/pi/updatePowerTune.sh
    for d in GPSTracks Gauges KTracks Logo Sounds exampleDash fonts graphics; do \
       cp -rd ${S}/$d/ ${D}/opt/PowerTune/
    done

    # PowerTune also expects these three asset folders directly under
    # /home/pi at runtime, separately from the /opt/PowerTune install
    # tree above. Logo and UserDashboards come from within exampleDash/,
    # not the top-level Logo/ (app branding/splash assets, already
    # installed to /opt/PowerTune above) -- this matches the source
    # repo's own updateUserDashboards.sh, which updates these same two
    # folders from these same exampleDash paths.
    install -d ${D}/home/pi/KTracks ${D}/home/pi/Logo ${D}/home/pi/UserDashboards
    cp -r ${S}/KTracks/. ${D}/home/pi/KTracks/
    cp -r ${S}/exampleDash/Logo/. ${D}/home/pi/Logo/
    cp -r ${S}/exampleDash/UserDashboards/. ${D}/home/pi/UserDashboards/

    install -m 0755 -p ${S}/LicenceRequest ${D}/home/pi/LicenceRequest

    # ECU protocol daemons (updatedaemons.sh: cp -r .../daemons/. /home/pi/daemons/)
    install -d ${D}/home/pi/daemons
    cp -r ${S}/daemons/. ${D}/home/pi/daemons/

    # Register the fonts system-wide so Qt/fontconfig can find them by
    # family name, not just the copy bundled alongside the app binary in
    # /opt/PowerTune/fonts above. Both paths below come from the source's
    # own scripts (installfonts.sh uses the PowertuneDigital subdir;
    # updatedaemons.sh copies flat into /usr/local/share/fonts), so both
    # are provided for parity with what the on-device scripts have done.
    install -d ${D}${datadir}/fonts/PowertuneDigital ${D}/usr/local/share/fonts
    cp -r ${S}/fonts/. ${D}${datadir}/fonts/PowertuneDigital/
    cp -r ${S}/fonts/. ${D}/usr/local/share/fonts/

    # udev rule for the PLMS Consult FTDI cable (registerPLMSCONSULT.sh).
    # The rule as checked in to Scripts/99-usbftdi.rules uses curly quotes
    # and the udev keyword predates ATTRS{} matching (SYSFS{} was removed years ago),
    # so as written it never matches anything; this is the same rule
    # rewritten with modern udev syntax and plain quotes.
    install -d ${D}${sysconfdir}/udev/rules.d
    cat <<EOF>${D}${sysconfdir}/udev/rules.d/99-usbftdi.rules
# For PLMS Developments Consult Cable FTDI FT232 & FT245 USB devices with Vendor ID = 0x0403, Product ID = 0xc7d9
SUBSYSTEM=="usb", ATTRS{idVendor}=="0403", ATTRS{idProduct}=="c7d9", RUN+="/sbin/modprobe -q ftdi_sio vendor=0x0403 product=0xc7d9"
EOF

    # Add sudoers config
    install -dm 0750 ${D}${sysconfdir}/sudoers.d
    cat<<EOF>${D}${sysconfdir}/sudoers.d/powertune
pi ALL=(ALL) ALL
EOF

    # Install InitV scripts
    for d in init.d rc3.d rc5.d; do \
        install -dm 0755 ${D}${sysconfdir}/${d}; \
    done
    cat <<EOF>${D}${sysconfdir}/init.d/powertune
#!/bin/sh
#export Enviroment variables
export LD_LIBRARY_PATH="/usr/local/lib/openssl/openssl/lib:\$LD_LIBRARY_PATH"
export LC_ALL=en_US.utf8
export QT_QPA_EGLFS_HIDECURSOR=1
export QT_QPA_EGLFS_ALWAYS_SET_MODE=1
export QT_QPA_EGLFS_KMS_ATOMIC=1
export QT_QPA_PLATFORM=eglfs

/home/pi/powertune-update.sh ||:

/home/pi/startdaemon.sh &
cd /opt/PowerTune
./PowertuneQMLGui -platform eglfs &

pgrep -x "PowertuneQMLGui" > /dev/null
if [ \$? -eq 1 ]; then
    echo "PowertuneQMLGui did not start properly, launching recovery script"
    cd /home/pi/Recovery/
    ./Recovery -platform eglfs &
    exit 1
fi
EOF
    chmod 0755 ${D}${sysconfdir}/init.d/powertune
    ln -s ../init.d/powertune ${D}${sysconfdir}/rc3.d/S010powertune
    ln -s ../init.d/powertune ${D}${sysconfdir}/rc5.d/S010powertune
}

FILES:${PN} += "/opt/PowerTune /home/pi/daemons /home/pi/*.sh /home/pi/KTracks /home/pi/Logo /home/pi/UserDashboards /home/pi/LicenceRequest"
FILES:${PN} += "${datadir}/fonts/PowertuneDigital /usr/local/share/fonts ${sysconfdir}/udev/rules.d/99-usbftdi.rules"

# The daemons/ binaries and LicenceRequest are prebuilt third-party ARM
# binaries checked directly into the source repo, not built by this
# recipe; some (e.g. HEFI) are already stripped, which the default QA
# check flags since it can't split off debug symbols for them.
INSANE_SKIP:${PN} += "already-stripped"
