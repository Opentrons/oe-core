#!/usr/bin/env bash

BUS=3
CONFADDR=0x74
VCOMADDR=0x28

echo "Using bus ${BUS}, conf reg ${CONFADDR}, vcom reg ${VCOMADDR}"

set -o pipefail

# i2cset [-f] [-y] [-m MASK] [-r] [-a] I2CBUS CHIP-ADDRESS DATA-ADDRESS [VALUE] ... [MODE]
#  I2CBUS is an integer or an I2C bus name
#  ADDRESS is an integer (0x03 - 0x77, or 0x00 - 0x7f if -a is given)
#  MODE is one of:
#    c (byte, no value)
#    b (byte data, default)
#    w (word data)
#    i (I2C block data)
#    s (SMBus block data)
#    Append p for SMBus PEC

function config_volwrite() {
  echo "Setting configuration"
  # CONF
  i2cset -y ${BUS} ${CONFADDR} 0x00 0x02
  # AVDD
  i2cset -y ${BUS} ${CONFADDR} 0x03 0x1f
  # SS2
  i2cset -y ${BUS} ${CONFADDR} 0x05 0x07
  # VGL
  i2cset -y ${BUS} ${CONFADDR} 0x07 0x05
  # VGH
  i2cset -y ${BUS} ${CONFADDR} 0x0a 0x00
  # SS4
  i2cset -y ${BUS} ${CONFADDR} 0x0b 0x03
  # OVP
  i2cset -y ${BUS} ${CONFADDR} 0x0e 0x00
  # VDET
  i2cset -y ${BUS} ${CONFADDR} 0x11 0x0f
}

function config_readback() {
p  echo "Config readback"
  echo "CONFIG: $(i2cget -y ${BUS} ${CONFADDR} 0x00)"
  echo "AVDD: $(i2cget -y ${BUS} ${CONFADDR} 0x03)"
  echo "SS2: $(i2cget -y ${BUS} ${CONFADDR} 0x05)"
  echo "VGL: $(i2cget -y ${BUS} ${CONFADDR} 0x07)"
  echo "VGH: $(i2cget -y ${BUS} ${CONFADDR} 0x0a)"
  echo "SS4: $(i2cget -y ${BUS} ${CONFADDR} 0x0b)"
  echo "OVP: $(i2cget -y ${BUS} ${CONFADDR} 0x0e)"
  echo "VDET: $(i2cget -y ${BUS} ${CONFADDR} 0x11)"
}

function config_check() {
  echo "Config check"
  [[ $(i2cget -y ${BUS} ${CONFADDR} 0x00) == 0x02 ]] || return 1
  [[ $(i2cget -y ${BUS} ${CONFADDR} 0x03) == 0x1f ]] || return 1
  [[ $(i2cget -y ${BUS} ${CONFADDR} 0x05) == 0x07 ]] || return 1
  [[ $(i2cget -y ${BUS} ${CONFADDR} 0x07) == 0x05 ]] || return 1
  [[ $(i2cget -y ${BUS} ${CONFADDR} 0x0a) == 0x00 ]] || return 1
  [[ $(i2cget -y ${BUS} ${CONFADDR} 0x0b) == 0x03 ]] || return 1
  [[ $(i2cget -y ${BUS} ${CONFADDR} 0x0e) == 0x00 ]] || return 1
  [[ $(i2cget -y ${BUS} ${CONFADDR} 0x11) == 0x0f ]] || return 1
  return 0
}

function config_save() {
  echo "Saving config"
  i2cset -y ${BUS} ${CONFADDR} 0xff 0x80
}

function config_read_ram() {
  echo "Configuration will be read from RAM"
  i2cset -y ${BUS} ${CONFADDR} 0xff 0x00
}

function config_read_rom() {
  echo "Configuration will be read from ROM"
  i2cset -y ${BUS} ${CONFADDR} 0xff 0x01
}

function vcom_write_both() {
  echo "VCOM will write to both IVI and WR and read IVI"
  i2cset -y ${BUS} ${VCOMADDR} 0x02 0x00
}

function vcom_write_wr_only() {
  echo "VCOM will write/read to wiper register only"
  i2cset -y ${BUS} ${VCOMADDR} 0x02 0x80
}

function vcom_write() {
  echo "Setting vcom"
  i2cset -y ${BUS} ${VCOMADDR} 0x00 0x37
}

vcom_readback() {
  echo "VCOM data"
  echo "VCOM: $(i2cget -y ${BUS} ${VCOMADDR} 0x00)"
}

function config_write_and_save() {
  echo "Writing and saving config"
  config_volwrite
  config_save
}

config_check && echo "Config OK" || config_write_and_save

vcom_write_both
vcom_write
vcom_readback

echo "Done"
exit 0
