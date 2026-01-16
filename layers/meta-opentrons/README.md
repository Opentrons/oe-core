# meta-opentrons: recipes for the opentrons ot3


This OpenEmbedded metalayer adds support for the opentrons ot3, including
- Recipes for the opentrons robot server, and other servers, from https://github.com/Opentrons/opentrons
- Recipes for non-upstreamed dependencies of the above
- Image recipes and configuration

It is not really useful on its own, only as part of a larger openembedded build. The
primary recipes that are useful in such a build and are provided here are the opentrons
recipes and the image.
