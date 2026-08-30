# Bake the Pi4 boot-diagnostics-screen fix into the shipped EEPROM firmware.
#
# disable_splash=1 in config.txt only silences the GPU firmware's rainbow
# splash screen. The "Configure this Raspberry Pi 4 Model B" diagnostics
# screen comes from the bootloader itself, which lives in the on-board SPI
# EEPROM and is configured independently via DISABLE_HDMI/DISABLE_SPLASH
# keys in the bootloader's own boot config (not config.txt).
#
# Setting DISABLE_HDMI=1 stops the bootloader from touching HDMI at all, so
# nothing is displayed until Linux's own KMS driver (dtoverlay=vc4-kms-v3d)
# takes over.
#
# This patches the config baked into the Pi4 (BCM2711) "default" and
# "latest" firmware images shipped by this recipe, so the fix is applied
# automatically whenever rpi-eeprom-update is run on a unit -- no manual
# per-board recovery.bin flash required. It does not itself trigger a
# flash; nothing here runs rpi-eeprom-update automatically.
#
# Caveat (rpi-eeprom issue #466, closed but unclear if formally fixed):
# DISABLE_HDMI=1 has been reported to cause the bootloader to ignore
# BOOT_ORDER fallback. Verify any BOOT_ORDER-dependent boot path (e.g. USB
# fallback if SD fails) still works before relying on this fleet-wide.
#
# On top of that, a first-boot service (rpi-eeprom-diagnostics-fix) stages
# this corrected firmware via `rpi-eeprom-update -d -f <image>` and
# reboots once to let it apply. A plain `rpi-eeprom-update -a` would NOT
# pick this up on its own: the auto-update path only flashes when the
# candidate image's BUILD_TIMESTAMP is newer than what's installed, and
# our patch only changes the config section, not the bootloader binary --
# so on a board already at that exact firmware version, the version-gated
# path silently does nothing. `-d -f` forces the flash regardless of
# version. The service is chip-checked (BCM2711 only) and stamp-guarded
# (runs once ever) so it's harmless to ship on Pi5 images too.

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://rpi-eeprom-diagnostics-fix \
    file://rpi-eeprom-diagnostics-fix.init \
    file://rpi-eeprom-diagnostics-fix.service \
"

inherit ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'systemd', 'update-rc.d', d)}

INITSCRIPT_NAME = "rpi-eeprom-diagnostics-fix"
INITSCRIPT_PARAMS = "start 99 S ."

SYSTEMD_SERVICE:${PN} = "rpi-eeprom-diagnostics-fix.service"

FILES:${PN} += "${sysconfdir}/init.d/rpi-eeprom-diagnostics-fix \
                 ${systemd_system_unitdir}/rpi-eeprom-diagnostics-fix.service \
"

do_install:append() {
    for release in default latest; do
        eeprom_dir="${D}${base_libdir}/firmware/raspberrypi/bootloader-2711/${release}"
        for img in "${eeprom_dir}"/pieeprom-*.bin; do
            [ -e "${img}" ] || continue

            bootconf="${WORKDIR}/bootconf-${release}.txt"
            # Preserve the existing config, dropping any prior DISABLE_HDMI/
            # DISABLE_SPLASH lines so we don't end up with duplicate keys.
            python3 "${S}/rpi-eeprom-config" "${img}" \
                | grep -v -e '^DISABLE_HDMI=' -e '^DISABLE_SPLASH=' > "${bootconf}"
            echo "DISABLE_SPLASH=1" >> "${bootconf}"
            echo "DISABLE_HDMI=1" >> "${bootconf}"

            python3 "${S}/rpi-eeprom-config" --config "${bootconf}" --out "${img}.new" "${img}"
            mv "${img}.new" "${img}"
        done
    done

    install -d ${D}${sbindir}
    install -m 0755 ${WORKDIR}/rpi-eeprom-diagnostics-fix ${D}${sbindir}/rpi-eeprom-diagnostics-fix
    sed -i -e 's#@BASE_LIBDIR@#${base_libdir}#' ${D}${sbindir}/rpi-eeprom-diagnostics-fix

    if ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'true', 'false', d)}; then
        install -d ${D}${systemd_system_unitdir}
        install -m 0644 ${WORKDIR}/rpi-eeprom-diagnostics-fix.service ${D}${systemd_system_unitdir}/rpi-eeprom-diagnostics-fix.service
    fi
    if ${@bb.utils.contains('DISTRO_FEATURES', 'sysvinit', 'true', 'false', d)}; then
        install -d ${D}${sysconfdir}/init.d
        install -m 0755 ${WORKDIR}/rpi-eeprom-diagnostics-fix.init ${D}${sysconfdir}/init.d/rpi-eeprom-diagnostics-fix
    fi
}
