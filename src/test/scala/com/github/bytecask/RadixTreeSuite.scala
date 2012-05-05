/*
* Copyright 2011 P.Budzik
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* User: przemek
* Date: 7/2/11
* Time: 12:07 PM
*/

package com.github.bytecask

import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import util.Random

class RadixTreeSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  var tree: RadixTree[Int] = _

  test("insert, size") {
    tree.insert("foo", 1)
    tree.insert("foo/d", 11)
    tree.insert("foo/c", 4)
    tree.insert("foo/a", 2)
    tree.insert("foo/b", 3)
    assert(tree.getTotalKeysSize == 8)
    println(tree.toString)
  }

  test("insert, find") {

    tree.insert("foo", 1)
    tree.insert("food", 11)
    tree.insert("fooc", 4)
    tree.insert("fooa", 2)
    tree.insert("foobar", 3)

    assert(tree.find("foo") == Some(1))
    assert(tree.find("fooa") == Some(2))
    assert(tree.find("foobar") == Some(3))
    assert(tree.find("fooc") == Some(4))
    assert(tree.find("food") == Some(11))
    assert(tree.find("xoooo") == None)

    tree.insert("foo", 0)
    assert(tree.find("foo") == Some(0))
    assert(tree.find("fooa") == Some(2))
    assert(tree.find("foobar") == Some(3))
    assert(tree.find("fooc") == Some(4))
    assert(tree.find("food") == Some(11))
    assert(tree.find("xoooo") == None)
  }

  test("insert, delete, find") {
    tree.insert("11.a", 1)
    tree.insert("11.b", 11)
    tree.insert("11.c", 4)

    tree.delete("11.a")
    tree.delete("11.c")

    assert(tree.find("11.a") == None)
    assert(tree.find("11.b") == Some(11))
    assert(tree.find("11.c") == None)

    assert(tree.getTotalKeysSize == 4)
  }

  test("lots of data") {
    val random = new Random
    val lines = dirs.split("\\n")
    lines.foreach(l => tree.insert(l.trim, random.nextInt()))
    assert(lines.size == tree.size)
    assert(dirs.size > tree.getTotalKeysSize)
    assert(!tree.find("/var/lib/dkms/fglrx/8.881/3.0.0-12-generic/x86_64/module").isEmpty)
    tree.delete("/var/mail")
    assert(tree.find("/var/mail").isEmpty)
    assert(!tree.find("/var/lib/dkms/fglrx/8.881/3.0.0-12-generic/x86_64/module").isEmpty)
  }

  test("iterator") {
    tree.insert("foo", 1)
    tree.insert("foo/d", 11)
    tree.insert("foo/c", 4)
    tree.insert("foo/a", 2)
    tree.insert("foo/b", 3)
    for ((k, v) <- tree.iterator) {
      assert(!k.isEmpty)
      assert(v > 0)
      println("k: " + k + ", v: " + v)
    }
  }

  override def beforeEach() {
    tree = new RadixTree[Int]()
  }

  val dirs = """/var/opt
  /var/backups
  /var/mail
  /var/lib/initscripts
  /var/lib/dkms/fglrx/8.881/3.0.0-12-generic/x86_64/log
  /var/lib/dkms/fglrx/8.881/3.0.0-12-generic/x86_64/module
  /var/lib/dkms/fglrx/8.881/3.0.0-12-generic/x86_64
  /var/lib/dkms/fglrx/8.881/3.0.0-12-generic
  /var/lib/dkms/fglrx/8.881/3.0.0-14-generic/x86_64/log
  /var/lib/dkms/fglrx/8.881/3.0.0-14-generic/x86_64/module
  /var/lib/dkms/fglrx/8.881/3.0.0-14-generic/x86_64
  /var/lib/dkms/fglrx/8.881/3.0.0-14-generic
  /var/lib/dkms/fglrx/8.881/3.0.0-16-generic/x86_64/log
  /var/lib/dkms/fglrx/8.881/3.0.0-16-generic/x86_64/module
  /var/lib/dkms/fglrx/8.881/3.0.0-16-generic/x86_64
  /var/lib/dkms/fglrx/8.881/3.0.0-16-generic
  /var/lib/dkms/fglrx/8.881/3.0.0-15-generic/x86_64/log
  /var/lib/dkms/fglrx/8.881/3.0.0-15-generic/x86_64/module
  /var/lib/dkms/fglrx/8.881/3.0.0-15-generic/x86_64
  /var/lib/dkms/fglrx/8.881/3.0.0-15-generic
  /var/lib/dkms/fglrx/8.881/3.0.0-19-generic/x86_64/log
  /var/lib/dkms/fglrx/8.881/3.0.0-19-generic/x86_64/module
  /var/lib/dkms/fglrx/8.881/3.0.0-19-generic/x86_64
  /var/lib/dkms/fglrx/8.881/3.0.0-19-generic
  /var/lib/dkms/fglrx/8.881/3.0.0-17-generic/x86_64/log
  /var/lib/dkms/fglrx/8.881/3.0.0-17-generic/x86_64/module
  /var/lib/dkms/fglrx/8.881/3.0.0-17-generic/x86_64
  /var/lib/dkms/fglrx/8.881/3.0.0-17-generic
  /var/lib/dkms/fglrx/8.881/build/patches
  /var/lib/dkms/fglrx/8.881/build/2.6.x/.tmp_versions
  /var/lib/dkms/fglrx/8.881/build/2.6.x
  /var/lib/dkms/fglrx/8.881/build
  /var/lib/dkms/fglrx/8.881/3.0.0-13-generic/x86_64/log
  /var/lib/dkms/fglrx/8.881/3.0.0-13-generic/x86_64/module
  /var/lib/dkms/fglrx/8.881/3.0.0-13-generic/x86_64
  /var/lib/dkms/fglrx/8.881/3.0.0-13-generic
  /var/lib/dkms/fglrx/8.881
  /var/lib/dkms/fglrx
  /var/lib/dkms
  /var/lib/usb_modeswitch
  /var/lib/ispell
  /var/lib/emacsen-common
  /var/lib/udisks
  /var/lib/libuuid
  /var/lib/plymouth
  /var/lib/misc
  /var/lib/upower
  /var/lib/NetworkManager
  /var/lib/avahi-autoipd
  /var/lib/acpi-support
  /var/lib/dhcp
  /var/lib/urandom
  /var/lib/usbutils
  /var/lib/mlocate
  /var/lib/update-notifier/user.d
  /var/lib/update-notifier
  /var/lib/snmp
  /var/lib/xml-core
  /var/lib/pam
  /var/lib/dbus
  /var/lib/anthy/mkworddic
  /var/lib/anthy/depgraph
  /var/lib/anthy
  /var/lib/samba
  /var/lib/ucf/cache
  /var/lib/ucf
  /var/lib/sgml-base
  /var/lib/pulseaudio
  /var/lib/gconf/debian.mandatory
  /var/lib/gconf/ubuntu-2d.mandatory
  /var/lib/gconf/debian.defaults
  /var/lib/gconf/ubuntu-2d.default
  /var/lib/gconf/defaults
  /var/lib/gconf
  /var/lib/ghostscript/CMap
  /var/lib/ghostscript/fonts
  /var/lib/ghostscript
  /var/lib/AccountsService/users
  /var/lib/AccountsService/icons
  /var/lib/AccountsService
  /var/lib/dpkg/parts
  /var/lib/dpkg/triggers
  /var/lib/dpkg/alternatives
  /var/lib/dpkg/updates
  /var/lib/dpkg/info
  /var/lib/dpkg
  /var/lib/ureadahead/debugfs
  /var/lib/ureadahead
  /var/lib/pycentral
  /var/lib/update-rc.d
  /var/lib/ubiquity/linux-boot-prober-cache
  /var/lib/ubiquity
  /var/lib/lightdm
  /var/lib/binfmts
  /var/lib/logrotate
  /var/lib/initramfs-tools
  /var/lib/emacs-snapshot/installed-subflavors
  /var/lib/emacs-snapshot
  /var/lib/vim/addons
  /var/lib/vim
  /var/lib/locales/supported.d
  /var/lib/locales
  /var/lib/mplayer/prefs
  /var/lib/mplayer
  /var/lib/insserv
  /var/lib/tex-common/fmtutil-cnf
  /var/lib/tex-common
  /var/lib/defoma/libwmf0.2-7.d
  /var/lib/defoma/x-ttcidfont-conf.d/dirs/TrueType
  /var/lib/defoma/x-ttcidfont-conf.d/dirs/CID
  /var/lib/defoma/x-ttcidfont-conf.d/dirs
  /var/lib/defoma/x-ttcidfont-conf.d
  /var/lib/defoma/fontconfig.d
  /var/lib/defoma/scripts
  /var/lib/defoma
  /var/lib/alsa
  /var/lib/update-manager
  /var/lib/flashplugin-installer
  /var/lib/colord
  /var/lib/ntpdate
  /var/lib/libreoffice/basis3.4/share/config
  /var/lib/libreoffice/basis3.4/share
  /var/lib/libreoffice/basis3.4/program
  /var/lib/libreoffice/basis3.4
  /var/lib/libreoffice/share/prereg/bundled/registry/com.sun.star.comp.deployment.sfwk.PackageRegistryBackend
  /var/lib/libreoffice/share/prereg/bundled/registry/com.sun.star.comp.deployment.component.PackageRegistryBackend
  /var/lib/libreoffice/share/prereg/bundled/registry/com.sun.star.comp.deployment.help.PackageRegistryBackend
  /var/lib/libreoffice/share/prereg/bundled/registry/com.sun.star.comp.deployment.executable.PackageRegistryBackend
  /var/lib/libreoffice/share/prereg/bundled/registry/com.sun.star.comp.deployment.bundle.PackageRegistryBackend
  /var/lib/libreoffice/share/prereg/bundled/registry/com.sun.star.comp.deployment.script.PackageRegistryBackend
  /var/lib/libreoffice/share/prereg/bundled/registry/com.sun.star.comp.deployment.configuration.PackageRegistryBackend
  /var/lib/libreoffice/share/prereg/bundled/registry
  /var/lib/libreoffice/share/prereg/bundled
  /var/lib/libreoffice/share/prereg
  /var/lib/libreoffice/share
  /var/lib/libreoffice
  /var/lib/sudo
  /var/lib/belocs
  /var/lib/apt-xapian-index
  /var/lib/doc-base/omf/initramfs-maintainer
  /var/lib/doc-base/omf/pnm2ppa-color
  /var/lib/doc-base/omf/install-docs-man
  /var/lib/doc-base/omf/xterm-ctlseqs
  /var/lib/doc-base/omf/dc
  /var/lib/doc-base/omf/sysstat-faq
  /var/lib/doc-base/omf/gnupginterface
  /var/lib/doc-base/omf/packet-filter
  /var/lib/doc-base/omf/python-pycurl
  /var/lib/doc-base/omf/django-tagging-overview
  /var/lib/doc-base/omf/cherrypy3-api
  /var/lib/doc-base/omf/man-db
  /var/lib/doc-base/omf/libsoundtouch0
  /var/lib/doc-base/omf/shared-mime-info
  /var/lib/doc-base/omf/xterm-faq
  /var/lib/doc-base/omf/python-policy
  /var/lib/doc-base/omf/pnm2ppa-ppa-networking
  /var/lib/doc-base/omf/fontconfig-user
  /var/lib/doc-base/omf/cups
  /var/lib/doc-base/omf/espeak-documentation
  /var/lib/doc-base/omf/tig
  /var/lib/doc-base/omf/cssutils
  /var/lib/doc-base/omf/foo2zjs
  /var/lib/doc-base/omf/nano-faq
  /var/lib/doc-base/omf/users-and-groups
  /var/lib/doc-base/omf/libpng12
  /var/lib/doc-base/omf/dvd+rw-tools
  /var/lib/doc-base/omf/time
  /var/lib/doc-base/omf/pnm2ppa-calibrate
  /var/lib/doc-base/omf/mc-faq
  /var/lib/doc-base/omf/doc-base
  /var/lib/doc-base/omf/nat
  /var/lib/doc-base/omf/xapian-python-docs
  /var/lib/doc-base/omf/kbd-font-formats
  /var/lib/doc-base/omf
  /var/lib/doc-base/documents
  /var/lib/doc-base/info
  /var/lib/doc-base
  /var/lib/dictionaries-common/ispell
  /var/lib/dictionaries-common/wordlist
  /var/lib/dictionaries-common
  /var/lib/polkit-1
  /var/lib/hp
  /var/lib/xkb
  /var/lib/xfonts
  /var/lib/apt/lists/partial
  /var/lib/apt/lists
  /var/lib/apt/periodic
  /var/lib/apt/keyrings
  /var/lib/apt/mirrors/partial
  /var/lib/apt/mirrors
  /var/lib/apt
  /var/lib/os-prober
  /var/lib
  /var/spool/anacron
  /var/spool/plymouth
  /var/spool/cron/crontabs
  /var/spool/cron/atspool
  /var/spool/cron/atjobs
  /var/spool/cron
  /var/spool/cups
  /var/spool/libreoffice/uno_packages/cache
  /var/spool/libreoffice/uno_packages
  /var/spool/libreoffice
  /var/spool
  /var/local
  /var/log/news
  /var/log/speech-dispatcher
  /var/log/dist-upgrade
  /var/log/fsck
  /var/log/ConsoleKit
  /var/log/samba
  /var/log/cups
  /var/log/lightdm
  /var/log/sysstat
  /var/log/unattended-upgrades
  /var/log/installer
  /var/log/apt
  /var/log
  /var/crash
  /var/games
  /var/cache/software-center/xapian
  /var/cache/software-center
  /var/cache/pm-utils
  /var/cache/jockey
  /var/cache/fontconfig
  /var/cache/man/da/cat1
  /var/cache/man/da
  /var/cache/man/et/cat1
  /var/cache/man/et
  /var/cache/man/lv/cat1
  /var/cache/man/lv
  /var/cache/man/bn/cat1
  /var/cache/man/bn
  /var/cache/man/fa/cat1
  /var/cache/man/fa
  /var/cache/man/cat8
  /var/cache/man/ml/cat1
  /var/cache/man/ml
  /var/cache/man/cat2
  /var/cache/man/sl/cat8
  /var/cache/man/sl/cat1
  /var/cache/man/sl
  /var/cache/man/io/cat1
  /var/cache/man/io
  /var/cache/man/bs/cat1
  /var/cache/man/bs
  /var/cache/man/uk/cat1
  /var/cache/man/uk
  /var/cache/man/ta/cat1
  /var/cache/man/ta
  /var/cache/man/te/cat1
  /var/cache/man/te
  /var/cache/man/id/cat8
  /var/cache/man/id/cat5
  /var/cache/man/id/cat1
  /var/cache/man/id
  /var/cache/man/he/cat1
  /var/cache/man/he
  /var/cache/man/pt/cat8
  /var/cache/man/pt/cat5
  /var/cache/man/pt/cat1
  /var/cache/man/pt
  /var/cache/man/nl/cat8
  /var/cache/man/nl/cat5
  /var/cache/man/nl/cat1
  /var/cache/man/nl
  /var/cache/man/pt_BR/cat8
  /var/cache/man/pt_BR/cat5
  /var/cache/man/pt_BR/cat1
  /var/cache/man/pt_BR
  /var/cache/man/tr/cat8
  /var/cache/man/tr/cat5
  /var/cache/man/tr/cat1
  /var/cache/man/tr
  /var/cache/man/cat7
  /var/cache/man/fi/cat1
  /var/cache/man/fi
  /var/cache/man/ko/cat8
  /var/cache/man/ko/cat5
  /var/cache/man/ko/cat1
  /var/cache/man/ko
  /var/cache/man/ast/cat1
  /var/cache/man/ast
  /var/cache/man/sv/cat8
  /var/cache/man/sv/cat5
  /var/cache/man/sv/cat1
  /var/cache/man/sv
  /var/cache/man/bg/cat1
  /var/cache/man/bg
  /var/cache/man/cat3
  /var/cache/man/es/cat8
  /var/cache/man/es/cat5
  /var/cache/man/es/cat1
  /var/cache/man/es
  /var/cache/man/ru/cat8
  /var/cache/man/ru/cat5
  /var/cache/man/ru/cat1
  /var/cache/man/ru
  /var/cache/man/ku/cat1
  /var/cache/man/ku
  /var/cache/man/fo/cat1
  /var/cache/man/fo
  /var/cache/man/local/cat1
  /var/cache/man/local
  /var/cache/man/ca@valencia/cat1
  /var/cache/man/ca@valencia
  /var/cache/man/en_AU/cat1
  /var/cache/man/en_AU
  /var/cache/man/ja/cat8
  /var/cache/man/ja/cat5
  /var/cache/man/ja/cat1
  /var/cache/man/ja
  /var/cache/man/ps/cat1
  /var/cache/man/ps
  /var/cache/man/vi/cat1
  /var/cache/man/vi
  /var/cache/man/it/cat8
  /var/cache/man/it/cat5
  /var/cache/man/it/cat1
  /var/cache/man/it
  /var/cache/man/fr/cat8
  /var/cache/man/fr/cat7
  /var/cache/man/fr/cat3
  /var/cache/man/fr/cat5
  /var/cache/man/fr/cat1
  /var/cache/man/fr
  /var/cache/man/sr/cat1
  /var/cache/man/sr
  /var/cache/man/ms/cat1
  /var/cache/man/ms
  /var/cache/man/th/cat1
  /var/cache/man/th
  /var/cache/man/pl/cat8
  /var/cache/man/pl/cat5
  /var/cache/man/pl/cat1
  /var/cache/man/pl
  /var/cache/man/cat4
  /var/cache/man/eu/cat1
  /var/cache/man/eu
  /var/cache/man/lt/cat1
  /var/cache/man/lt
  /var/cache/man/ca/cat1
  /var/cache/man/ca
  /var/cache/man/nb/cat1
  /var/cache/man/nb
  /var/cache/man/de/cat8
  /var/cache/man/de/cat7
  /var/cache/man/de/cat5
  /var/cache/man/de/cat1
  /var/cache/man/de
  /var/cache/man/sk/cat1
  /var/cache/man/sk
  /var/cache/man/el/cat1
  /var/cache/man/el
  /var/cache/man/fr.ISO8859-1/cat8
  /var/cache/man/fr.ISO8859-1/cat7
  /var/cache/man/fr.ISO8859-1
  /var/cache/man/eo/cat1
  /var/cache/man/eo
  /var/cache/man/hu/cat8
  /var/cache/man/hu/cat5
  /var/cache/man/hu/cat1
  /var/cache/man/hu
  /var/cache/man/cat5
  /var/cache/man/ro/cat1
  /var/cache/man/ro
  /var/cache/man/cs/cat8
  /var/cache/man/cs/cat7
  /var/cache/man/cs/cat5
  /var/cache/man/cs/cat1
  /var/cache/man/cs
  /var/cache/man/fr.UTF-8/cat8
  /var/cache/man/fr.UTF-8/cat7
  /var/cache/man/fr.UTF-8
  /var/cache/man/cat6
  /var/cache/man/cat1
  /var/cache/man/sq/cat1
  /var/cache/man/sq
  /var/cache/man/zh_TW/cat8
  /var/cache/man/zh_TW/cat5
  /var/cache/man/zh_TW/cat1
  /var/cache/man/zh_TW
  /var/cache/man/gl/cat1
  /var/cache/man/gl
  /var/cache/man/oc/cat1
  /var/cache/man/oc
  /var/cache/man/ar/cat1
  /var/cache/man/ar
  /var/cache/man/zh_HK/cat1
  /var/cache/man/zh_HK
  /var/cache/man/ug/cat1
  /var/cache/man/ug
  /var/cache/man/zh_CN/cat8
  /var/cache/man/zh_CN/cat5
  /var/cache/man/zh_CN/cat1
  /var/cache/man/zh_CN
  /var/cache/man/hr/cat1
  /var/cache/man/hr
  /var/cache/man/cy/cat1
  /var/cache/man/cy
  /var/cache/man/en_GB/cat1
  /var/cache/man/en_GB
  /var/cache/man/nn/cat1
  /var/cache/man/nn
  /var/cache/man
  /var/cache/anthy
  /var/cache/samba
  /var/cache/cups/rss
  /var/cache/cups
  /var/cache/lightdm/dmrc
  /var/cache/lightdm
  /var/cache/pppconfig
  /var/cache/flashplugin-installer
  /var/cache/apt-xapian-index/index.2
  /var/cache/apt-xapian-index
  /var/cache/dictionaries-common
  /var/cache/debconf
  /var/cache/ldconfig
  /var/cache/git
  /var/cache/apt/archives/partial
  /var/cache/apt/archives
  /var/cache/apt
  /var/cache
  /var/tmp
  /var
  /boot/grub/locale
  /boot/grub
  /boot"""
}