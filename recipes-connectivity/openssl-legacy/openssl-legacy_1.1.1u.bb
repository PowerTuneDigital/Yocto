SUMMARY = "Legacy OpenSSL 1.1.1, installed alongside the system OpenSSL for PowerTune"
DESCRIPTION = "PowerTune's Qt application needs OpenSSL 1.1.1's ABI, which is \
incompatible with the OpenSSL 3.x shipped as this image's system libcrypto/libssl. \
This cross-builds OpenSSL 1.1.1u into a private prefix so both versions can coexist, \
replacing the hand-built compiled_perl_openssl.tar.gz previously deployed from \
https://github.com/PowerTuneDigital/YoctoExtraPackages by hand after imaging."
HOMEPAGE = "http://www.openssl.org/"
SECTION = "libs/network"

LICENSE = "OpenSSL"
LIC_FILES_CHKSUM = "file://LICENSE;md5=d343e62fc9c833710bbbed25f27364c8"

SRC_URI = "https://github.com/openssl/openssl/releases/download/OpenSSL_1_1_1u/openssl-${PV}.tar.gz"
SRC_URI[sha256sum] = "e2f8d84b523eecd06c7be7626830370300fbcc15386bf5142d72758f6963ebc6"

S = "${WORKDIR}/openssl-${PV}"
B = "${WORKDIR}/build"
do_configure[cleandirs] = "${B}"

# Private, non-conflicting prefix so this coexists with the system OpenSSL
# 3.x (recipes-connectivity/openssl in poky) instead of replacing it. This
# matches the on-device layout the previous hand-built tarball used, which
# /etc/init.d/powertune's LD_LIBRARY_PATH already points at (see
# recipes-qt/powertune/powertune_git.bb).
OPENSSL_LEGACY_PREFIX = "/usr/local/lib/openssl/openssl"

do_configure () {
    os=${HOST_OS}
    case $os in
    linux-gnueabi | linux-gnuspe | linux-musleabi | linux-muslspe | linux-musl)
        os=linux
        ;;
    esac
    target="$os-${HOST_ARCH}"
    case $target in
    linux-arm*)
        target=linux-armv4
        ;;
    linux-aarch64*)
        target=linux-aarch64
        ;;
    linux-i?86 | linux-viac3)
        target=linux-x86
        ;;
    linux-x86_64)
        target=linux-x86_64
        ;;
    esac

    perl ${S}/Configure shared \
        --prefix=${OPENSSL_LEGACY_PREFIX} \
        --openssldir=${OPENSSL_LEGACY_PREFIX}/ssl \
        $target
}

do_install () {
    oe_runmake DESTDIR="${D}" install_sw install_ssldirs

    # The previous manual install also exposed the CLI directly on PATH at
    # /usr/local/bin/openssl (YoctoExtraPackages/installpackages.sh); keep that.
    install -d ${D}/usr/local/bin
    ln -sf ../lib/openssl/openssl/bin/openssl ${D}/usr/local/bin/openssl

    # Make the legacy libs/CLI discoverable for interactive shells, the
    # same way installpackages.sh's /etc/profile.d/yocto_extra_packages.sh
    # did. powertune's own init script sets its own LD_LIBRARY_PATH and
    # does not depend on this.
    install -d ${D}${sysconfdir}/profile.d
    cat <<EOF > ${D}${sysconfdir}/profile.d/openssl-legacy.sh
export PATH="/usr/local/bin:\$PATH"
export LD_LIBRARY_PATH="${OPENSSL_LEGACY_PREFIX}/lib:\$LD_LIBRARY_PATH"
EOF
}

FILES:${PN} += "/usr/local/lib/openssl /usr/local/bin/openssl ${sysconfdir}/profile.d/openssl-legacy.sh"

# This is deliberately one self-contained package at a private prefix, not
# a normal system libdir -- skip the usual -dev/-dbg/-staticdev split
# machinery and the QA checks that assume every "/lib/" path segment is
# ${libdir} (it structurally looks like one here, but isn't).
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INSANE_SKIP:${PN} += "staticdev dev-so libdir"
