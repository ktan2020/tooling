#!/bin/sh

alias NN="nano"

export VISUAL=nano
export EDITOR=nano
export TERM="xterm"
export USERHOME=/home/vagrant

export PATH=$USERHOME/workspace/bin:$USERHOME/workspace/instantclient_10_2:$PATH
export LD_LIBRARY_PATH=$USERHOME/workspace/lib:$USERHOME/workspace/instantclient_10_2
export ORACLE_HOME=$USERHOME/workspace/instantclient_10_2

export PKG_CONFIG_PATH=$USERHOME/workspace/lib/pkgconfig


if [[ ! -e $HOME/.screenrc || ! -e $HOME/.nanorc ]]; then
    echo " ... copying dot files over ... "
    cp /vagrant/pkgs/.* $HOME &> /dev/null
    cp /vagrant/pkgs/rc-master.zip $USERHOME/temp
    (cd $USERHOME/temp && unzip -o rc-master.zip && rm -rf $HOME/.nano && mv -f nanorc-master $HOME/.nano && cat $HOME/.nano/nanorc >> $HOME/.nanorc)
fi

cpanm --local-lib=~/perl5 local::lib && eval $(perl -I ~/perl5/lib/perl5/ -Mlocal::lib)
if [[ -n $UPDATE_NEEDED ]]; then 
    cpanm --self-upgrade
    gem update
    python -m pip install -U pip
fi

screen
