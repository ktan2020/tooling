FROM centos:7

LABEL description="Vagrant aws smoke test"

WORKDIR /tmp
ADD my_test_key.pem /tmp

ENV NAME GRANITE_ACCESS_KEY_ID 
ENV NAME GRANITE_SECRET_ACCESS_KEY
ENV NAME GRANITE_KEYPAIR_NAME
ENV NAME GRANITE_PRIVATE_KEY_PATH

RUN rpm -ivh https://releases.hashicorp.com/vagrant/2.0.3/vagrant_2.0.3_x86_64.rpm
RUN yum install -y ruby-devel autoconf automake gcc make gettext bzip2 glibc-devel glibc-headers 
RUN vagrant plugin install vagrant-aws 

#RUN vagrant box add dummy https://github.com/mitchellh/vagrant-aws/raw/master/dummy.box
RUN vagrant init centos/7

RUN sed -i 's|# config.vm.synced_folder.*|config.vm.synced_folder ".", "/vagrant", disabled: false|g' Vagrantfile
RUN sed -i 's|# config.vm.provision.*|config.vm.provision "shell", inline: "yum install -y gdb screen ruby-devel"|g' Vagrantfile

ENTRYPOINT ["vagrant"]
CMD ["up","--provider=aws"]
