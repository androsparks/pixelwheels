export PATH := $(PWD)/../../bin:$(PATH)

OUT_DIR ?= ../assets

TOOLS_DIR ?= ../../tools

FINAL_OUT_DIR ?= ../../android/assets

VEHICLES_SUBDIR := sprites/vehicles

# Path to aseprite or libresprite
ASEPRITE ?= aseprite

all: hud anims stills vehicles tiles helicopter ui map-icons input-icons

clean:
	rm -rf $(OUT_DIR)

#- Hud ------------------------------------------------------------------------
# Just enough to trigger the target
hud: $(OUT_DIR)/sprites/hud-pause.png

$(OUT_DIR)/sprites/hud-pause.png: sprites/hud/hud-pie-buttons.ase sprites/hud/hud-sides-buttons.ase sprites/hud/hud.py
	mkdir -p $(OUT_DIR)/sprites

	$(ASEPRITE) --batch --split-layers --trim sprites/hud/hud-pie-buttons.ase \
		--filename-format '{path}/hud-{layer}.{extension}' --save-as sprites/hud/whatever.png

	$(ASEPRITE) --batch --split-slices sprites/hud/hud-sides-buttons.ase \
		--filename-format '{path}/{slice}.{extension}' --save-as sprites/hud/whatever.png

	sprites/hud/hud.py $(OUT_DIR)/sprites

#- Still images ---------------------------------------------------------------
STILL_IMAGES := $(wildcard sprites/*.still.ase)
OUT_STILL_IMAGES := $(STILL_IMAGES:%.still.ase=$(OUT_DIR)/%.png)
stills: $(OUT_STILL_IMAGES)

$(OUT_DIR)/sprites/%.png: sprites/%.still.ase
	mkdir -p $(OUT_DIR)/sprites
	$(ASEPRITE) --batch $< --save-as $@

#- Anims ----------------------------------------------------------------------
ANIM_IMAGES := $(wildcard sprites/*.anim.ase)
OUT_ANIM_FIRST_FRAMES := $(ANIM_IMAGES:%.anim.ase=$(OUT_DIR)/%_0.png)

anims: $(OUT_ANIM_FIRST_FRAMES)

$(OUT_DIR)/sprites/%_0.png: sprites/%.anim.ase
	mkdir -p $(OUT_DIR)/sprites
	$(ASEPRITE) --batch $< --filename-format '{path}/{title}_{frame}.{extension}' --save-as $(patsubst %_0.png,%.png,$@)

#- Helicopter -----------------------------------------------------------------
helicopter: $(OUT_DIR)/sprites/helicopter-body.png

$(OUT_DIR)/sprites/helicopter-body.png: sprites/helicopter.ase
	mkdir -p $(OUT_DIR)/sprites
	$(ASEPRITE) --batch --split-layers --trim $< --filename-format '{path}/helicopter-{layer}.{extension}' --save-as $@

#- Vehicles -------------------------------------------------------------------
VEHICLE_IMAGES := $(wildcard sprites/vehicles/*.ase)
OUT_VEHICLE_IMAGES := $(VEHICLE_IMAGES:%.ase=$(OUT_DIR)/%.png)

vehicles: $(OUT_VEHICLE_IMAGES)

$(OUT_DIR)/sprites/vehicles/%.png: TMP_PNG = $(@:%.png=%-tmp.png)
$(OUT_DIR)/sprites/vehicles/%.png: sprites/vehicles/%.ase
	mkdir -p $(OUT_DIR)/sprites/vehicles
	$(ASEPRITE) --batch $< --save-as $(TMP_PNG)
	convert -rotate 90 $(TMP_PNG) $@
	rm $(TMP_PNG)

#- Tiles ----------------------------------------------------------------------
TILE_IMAGES := $(wildcard maps/*.ase)
OUT_TILE_IMAGES := $(TILE_IMAGES:%.ase=$(FINAL_OUT_DIR)/%.png)

tiles: $(OUT_TILE_IMAGES)

$(FINAL_OUT_DIR)/maps/%.png: TMP_PNG = $(@:%.png=%-tmp.png)
$(FINAL_OUT_DIR)/maps/%.png: maps/%.ase
	mkdir -p $(FINAL_OUT_DIR)/maps
	$(ASEPRITE) --batch $< --save-as $(TMP_PNG)
	convert -alpha set -channel RGBA \
		-fill '#22203460' -opaque '#ff00ff' \
		-fill '#ffffff20' -opaque '#00ffff' \
		$(TMP_PNG) - \
	| $(TOOLS_DIR)/pad-map-tiles --padding 2 --tilesize 64 --output $@
	rm $(TMP_PNG)

#- UI files -------------------------------------------------------------------
UI_IMAGES := $(wildcard ui/*.ase)
OUT_UI_IMAGES := $(UI_IMAGES:%.ase=$(OUT_DIR)/%.png)

ui: $(OUT_UI_IMAGES)

$(OUT_DIR)/ui/%.png: ui/%.ase
	mkdir -p $(OUT_DIR)/ui
	$(ASEPRITE) --batch $< --save-as $@

#- Map icons ------------------------------------------------------------------
MAP_ICONS := $(wildcard ui/map-icons/*.ase)
OUT_MAP_ICONS := $(MAP_ICONS:%.ase=$(OUT_DIR)/%.png)

map-icons: $(OUT_MAP_ICONS)

$(OUT_DIR)/ui/map-icons/%.png: ui/map-icons/%.ase
	mkdir -p $(OUT_DIR)/ui/map-icons
	$(ASEPRITE) --batch $< --save-as $@

#- Input icons ----------------------------------------------------------------
INPUT_ICONS := ui/input-icons/input-icons.ase
OUT_INPUT_ICONS_DIR := $(OUT_DIR)/ui/input-icons

input-icons: $(OUT_INPUT_ICONS_DIR)/sides.png

$(OUT_INPUT_ICONS_DIR)/sides.png: $(INPUT_ICONS)
	mkdir -p $(OUT_INPUT_ICONS_DIR)

	$(ASEPRITE) --batch --split-slices $< \
		--filename-format "$(OUT_INPUT_ICONS_DIR)/{slice}.{extension}" --save-as whatever.png
