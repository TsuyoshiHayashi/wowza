# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
    config.vm.define 'th'
    config.vm.box = 'hwdmedia/centos69-wowza47'
    config.vm.box_url = 'https://s3-eu-west-1.amazonaws.com/hwdmedia-resources/boxes/centos69-wowza47.box'

    config.vm.provider 'virtualbox' do |v|
        v.memory = 4096
        v.cpus = 2
    end

    config.hostmanager.enabled = true
    config.hostmanager.manage_host = true
    config.vm.hostname = 'th.vm'

    [1935, 8088].each do |port|
        config.vm.network :forwarded_port, guest: port, host: port, host_ip: 'localhost'
    end

    config.vm.synced_folder '.', '/home/vagrant/recorduploader', :mount_options => %w(dmode=775 fmode=664)

    config.vm.provision :shell, :name => 'vagrant-maven.ssh', :args => ['3.5.0'], :path => 'https://bitbucket.org/!api/2.0/snippets/hwdmedia/aKMeA/files/vagrant-maven.sh'
end
