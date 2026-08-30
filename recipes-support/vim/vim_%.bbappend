# This kirkstone-era vim recipe builds and installs /usr/bin/xxd (via
# ALTERNATIVE) and declares build-time PROVIDES = "xxd", but never
# declares the matching runtime RPROVIDES -- later poky releases add
# this. Without it, anything that RDEPENDS on "xxd" (e.g. rpi-eeprom,
# see recipes-bsp/rpi-eeprom) fails to resolve at all.
RPROVIDES:${PN} += "xxd"
