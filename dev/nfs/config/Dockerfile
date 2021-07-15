FROM erichough/nfs-server:latest

RUN apk update && apk add dhcp tftp-hpa
RUN mkdir -p /var/lib/dhcp \
    && touch /var/lib/dhcp/dhcpd.leases \
    && chmod 775 /var/lib/dhcp && chmod 664 /var/lib/dhcp/dhcpd.leases \
    && mkdir /var/run/dhcp-server
ADD dhcpd.conf /etc/dhcpd/dhcpd.conf
ADD exports /etc/exports
ADD run.sh /run.sh
RUN chmod a+x /run.sh

ENTRYPOINT ["/run.sh"]
CMD ["CHANGEME"]
