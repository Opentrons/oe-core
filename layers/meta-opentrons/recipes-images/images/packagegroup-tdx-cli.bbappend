# override the tpm2 package recommends. this has to be done like this rather than
# adding the contents to BAD_RECOMMENDATIONS because if the packages appear in
# the RRECOMMENDS at all, bitbake will choke if the source recipes aren't available
# somewhere, and we don't have the layers that provide these packages in our build
# since we don't use them.

RRECOMMENDS:packagegroup-tpm2-tdx-cli := ""
