// Copyright (c) 2012- PPSSPP Project.

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, version 2.0 or later versions.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License 2.0 for more details.

// A copy of the GPL 2.0 should have been included with the program.
// If not, see http://www.gnu.org/licenses/

// Official git repository and contact information can be found at
// https://github.com/hrydgard/ppsspp and http://www.ppsspp.org/.

#include <cstdio>
#include <cstdlib>
#include <locale.h>

#include "Common/StringUtils.h"
#include "Common/GPU/OpenGL/GLFeatures.h"
#include "Common/GPU/ShaderWriter.h"
#include "Common/GPU/thin3d.h"
#include "Core/Config.h"
#include "GPU/ge_constants.h"
#include "GPU/GPUState.h"
#include "GPU/Common/ShaderId.h"
#include "GPU/Common/ShaderUniforms.h"
#include "GPU/Common/VertexDecoderCommon.h"
#include "GPU/Common/VertexShaderGenerator.h"

#undef WRITE

#define WRITE(p, ...) p.F(__VA_ARGS__)

static const char * const boneWeightAttrDecl[9] = {
	"#ERROR#",
	"attribute mediump float w1;\n",
	"attribute mediump vec2 w1;\n",
	"attribute mediump vec3 w1;\n",
	"attribute mediump vec4 w1;\n",
	"attribute mediump vec4 w1;\nattribute mediump float w2;\n",
	"attribute mediump vec4 w1;\nattribute mediump vec2 w2;\n",
	"attribute mediump vec4 w1;\nattribute mediump vec3 w2;\n",
	"attribute mediump vec4 w1, w2;\n",
};

static const char * const boneWeightInDecl[9] = {
	"#ERROR#",
	"in mediump float w1;\n",
	"in mediump vec2 w1;\n",
	"in mediump vec3 w1;\n",
	"in mediump vec4 w1;\n",
	"in mediump vec4 w1;\nin mediump float w2;\n",
	"in mediump vec4 w1;\nin mediump vec2 w2;\n",
	"in mediump vec4 w1;\nin mediump vec3 w2;\n",
	"in mediump vec4 w1, w2;\n",
};

const char *boneWeightAttrDeclHLSL[9] = {
	"#ERROR boneWeightAttrDecl#\n",
	"float  a_w1:TEXCOORD1;\n",
	"vec2 a_w1:TEXCOORD1;\n",
	"vec3 a_w1:TEXCOORD1;\n",
	"vec4 a_w1:TEXCOORD1;\n",
	"vec4 a_w1:TEXCOORD1;\n  float a_w2:TEXCOORD2;\n",
	"vec4 a_w1:TEXCOORD1;\n  vec2 a_w2:TEXCOORD2;\n",
	"vec4 a_w1:TEXCOORD1;\n  vec3 a_w2:TEXCOORD2;\n",
	"vec4 a_w1:TEXCOORD1;\n  vec4 a_w2:TEXCOORD2;\n",
};

const char *boneWeightAttrInitHLSL[9] = {
	"  #ERROR#\n",
	"  vec4 w1 = vec4(In.a_w1, 0.0, 0.0, 0.0);\n",
	"  vec4 w1 = vec4(In.a_w1.xy, 0.0, 0.0);\n",
	"  vec4 w1 = vec4(In.a_w1.xyz, 0.0);\n",
	"  vec4 w1 = In.a_w1;\n",
	"  vec4 w1 = In.a_w1;\n  vec4 w2 = vec4(In.a_w2, 0.0, 0.0, 0.0);\n",
	"  vec4 w1 = In.a_w1;\n  vec4 w2 = vec4(In.a_w2.xy, 0.0, 0.0);\n",
	"  vec4 w1 = In.a_w1;\n  vec4 w2 = vec4(In.a_w2.xyz, 0.0);\n",
	"  vec4 w1 = In.a_w1;\n  vec4 w2 = In.a_w2;\n",
};

// Depth range and viewport
//
// After the multiplication with the projection matrix, we have a 4D vector in clip space.
// In OpenGL, Z is from -1 to 1, while in D3D, Z is from 0 to 1.
// PSP appears to use the OpenGL convention. As Z is from -1 to 1, and the viewport is represented
// by a center and a scale, to find the final Z value, all we need to do is to multiply by ZScale and
// add ZCenter - these are properly scaled to directly give a Z value in [0, 65535].
//
// z = vec.z * ViewportZScale + ViewportZCenter;
//
// That will give us the final value between 0 and 65535, which we can simply floor to simulate
// the limited precision of the PSP's depth buffer. Then we convert it back:
// z = floor(z);
//
// vec.z = (z - ViewportZCenter) / ViewportZScale;
//
// Now, the regular machinery will take over and do the calculation again.
//
// Depth is not clipped to the viewport, but does clip to "minz" and "maxz".  It may also be clamped
// to 0 and 65535 if a depth clamping/clipping flag is set (x/y clipping is performed only if depth
// needs to be clamped.)
//
// Additionally, depth is clipped to negative z based on vec.z (before viewport), at -1.
//
// All this above is for full transform mode.
// In through mode, the Z coordinate just goes straight through and there is no perspective division.
// We simulate this of course with pretty much an identity matrix. Rounding Z becomes very easy.
//
// TODO: Skip all this if we can actually get a 16-bit depth buffer along with stencil, which
// is a bit of a rare configuration, although quite common on mobile.

static const char * const boneWeightDecl[9] = {
	"#ERROR#",
	"layout(location = 3) in float w1;\n",
	"layout(location = 3) in vec2 w1;\n",
	"layout(location = 3) in vec3 w1;\n",
	"layout(location = 3) in vec4 w1;\n",
	"layout(location = 3) in vec4 w1;\nlayout(location = 4) in float w2;\n",
	"layout(location = 3) in vec4 w1;\nlayout(location = 4) in vec2 w2;\n",
	"layout(location = 3) in vec4 w1;\nlayout(location = 4) in vec3 w2;\n",
	"layout(location = 3) in vec4 w1;\nlayout(location = 4) in vec4 w2;\n",
};

extern const char *vulkan_glsl_preamble_vs;
extern const char *hlsl_preamble_vs;

bool GenerateVertexShader(const VShaderID &id, char *buffer, const ShaderLanguageDesc &compat, Draw::Bugs bugs, uint32_t *attrMask, uint64_t *uniformMask, std::string *errorString) {
	*attrMask = 0;
	*uniformMask = 0;

	bool highpFog = false;
	bool highpTexcoord = false;

	std::vector<const char*> gl_exts;
	if (ShaderLanguageIsOpenGL(compat.shaderLanguage)) {
		if (gl_extensions.EXT_gpu_shader4) {
			gl_exts.push_back("#extension GL_EXT_gpu_shader4 : enable");
		}
		if (gl_extensions.EXT_clip_cull_distance && id.Bit(VS_BIT_VERTEX_RANGE_CULLING)) {
			gl_exts.push_back("#extension GL_EXT_clip_cull_distance : enable");
		}
		if (gl_extensions.APPLE_clip_distance && id.Bit(VS_BIT_VERTEX_RANGE_CULLING)) {
			gl_exts.push_back("#extension GL_APPLE_clip_distance : enable");
		}
	}
	ShaderWriter p(buffer, compat, ShaderStage::Vertex, gl_exts.data(), gl_exts.size());

	bool isModeThrough = id.Bit(VS_BIT_IS_THROUGH);
	bool lmode = id.Bit(VS_BIT_LMODE);
	bool doTexture = id.Bit(VS_BIT_DO_TEXTURE);

	GETexMapMode uvGenMode = static_cast<GETexMapMode>(id.Bits(VS_BIT_UVGEN_MODE, 2));
	bool doTextureTransform = uvGenMode == GE_TEXMAP_TEXTURE_MATRIX;

	// this is only valid for some settings of uvGenMode
	GETexProjMapMode uvProjMode = static_cast<GETexProjMapMode>(id.Bits(VS_BIT_UVPROJ_MODE, 2));
	bool doShadeMapping = uvGenMode == GE_TEXMAP_ENVIRONMENT_MAP;

	bool flatBug = bugs.Has(Draw::Bugs::BROKEN_FLAT_IN_SHADER) && g_Config.bVendorBugChecksEnabled;
	bool needsZWHack = bugs.Has(Draw::Bugs::EQUAL_WZ_CORRUPTS_DEPTH) && g_Config.bVendorBugChecksEnabled;
	bool doFlatShading = id.Bit(VS_BIT_FLATSHADE) && !flatBug;

	bool useHWTransform = id.Bit(VS_BIT_USE_HW_TRANSFORM);
	bool hasColor = id.Bit(VS_BIT_HAS_COLOR) || !useHWTransform;
	bool hasNormal = id.Bit(VS_BIT_HAS_NORMAL) && useHWTransform;
	bool hasTexcoord = id.Bit(VS_BIT_HAS_TEXCOORD) || !useHWTransform;
	bool enableFog = id.Bit(VS_BIT_ENABLE_FOG);
	bool flipNormal = id.Bit(VS_BIT_NORM_REVERSE);
	int ls0 = id.Bits(VS_BIT_LS0, 2);
	int ls1 = id.Bits(VS_BIT_LS1, 2);
	bool enableBones = id.Bit(VS_BIT_ENABLE_BONES) && useHWTransform;
	bool enableLighting = id.Bit(VS_BIT_LIGHTING_ENABLE);
	int matUpdate = id.Bits(VS_BIT_MATERIAL_UPDATE, 3);

	// Apparently we don't support bezier/spline together with bones.
	bool doBezier = id.Bit(VS_BIT_BEZIER) && !enableBones && useHWTransform;
	bool doSpline = id.Bit(VS_BIT_SPLINE) && !enableBones && useHWTransform;
	if (doBezier || doSpline) {
		if (!hasNormal) {
			// Bad usage.
			*errorString = "Invalid flags - tess requires normal.";
			return false;
		}
		if (compat.shaderLanguage == HLSL_D3D9 || compat.texelFetch == nullptr) {
			*errorString = "Tess not supported on this shader language version";
			return false;
		}
	}
	bool hasColorTess = id.Bit(VS_BIT_HAS_COLOR_TESS);
	bool hasTexcoordTess = id.Bit(VS_BIT_HAS_TEXCOORD_TESS);
	bool hasNormalTess = id.Bit(VS_BIT_HAS_NORMAL_TESS);
	bool flipNormalTess = id.Bit(VS_BIT_NORM_REVERSE_TESS);

	const char *shading = "";
	if (compat.glslES30 || compat.shaderLanguage == GLSL_VULKAN)
		shading = doFlatShading ? "flat " : "";

	DoLightComputation doLight[4] = { LIGHT_OFF, LIGHT_OFF, LIGHT_OFF, LIGHT_OFF };
	if (useHWTransform) {
		int shadeLight0 = doShadeMapping ? ls0 : -1;
		int shadeLight1 = doShadeMapping ? ls1 : -1;
		for (int i = 0; i < 4; i++) {
			if (i == shadeLight0 || i == shadeLight1)
				doLight[i] = LIGHT_SHADE;
			if (enableLighting && id.Bit(VS_BIT_LIGHT0_ENABLE + i))
				doLight[i] = LIGHT_FULL;
		}
	}

	int numBoneWeights = 0;
	int boneWeightScale = id.Bits(VS_BIT_WEIGHT_FMTSCALE, 2);
	if (enableBones) {
		numBoneWeights = 1 + id.Bits(VS_BIT_BONES, 3);
	}
	bool texCoordInVec3 = false;

	bool vertexRangeCulling = id.Bit(VS_BIT_VERTEX_RANGE_CULLING);

	if (compat.shaderLanguage == GLSL_VULKAN) {
		WRITE(p, "\n");
		WRITE(p, "layout (std140, set = 0, binding = 3) uniform baseVars {\n%s};\n", ub_baseStr);
		if (enableLighting || doShadeMapping)
			WRITE(p, "layout (std140, set = 0, binding = 4) uniform lightVars {\n%s};\n", ub_vs_lightsStr);
		if (enableBones)
			WRITE(p, "layout (std140, set = 0, binding = 5) uniform boneVars {\n%s};\n", ub_vs_bonesStr);

		if (enableBones) {
			WRITE(p, "%s", boneWeightDecl[numBoneWeights]);
		}

		if (useHWTransform)
			WRITE(p, "layout (location = %d) in vec3 position;\n", (int)PspAttributeLocation::POSITION);
		else
			// we pass the fog coord in w
			WRITE(p, "layout (location = %d) in vec4 position;\n", (int)PspAttributeLocation::POSITION);

		if (useHWTransform && hasNormal)
			WRITE(p, "layout (location = %d) in vec3 normal;\n", (int)PspAttributeLocation::NORMAL);

		if (doTexture && hasTexcoord) {
			if (!useHWTransform && doTextureTransform && !isModeThrough) {
				WRITE(p, "layout (location = %d) in vec3 texcoord;\n", (int)PspAttributeLocation::TEXCOORD);
				texCoordInVec3 = true;
			} else
				WRITE(p, "layout (location = %d) in vec2 texcoord;\n", (int)PspAttributeLocation::TEXCOORD);
		}
		if (hasColor) {
			WRITE(p, "layout (location = %d) in vec4 color0;\n", (int)PspAttributeLocation::COLOR0);
			if (lmode && !useHWTransform)  // only software transform supplies color1 as vertex data
				WRITE(p, "layout (location = %d) in vec3 color1;\n", (int)PspAttributeLocation::COLOR1);
		}

		WRITE(p, "layout (location = 1) %sout lowp vec4 v_color0;\n", shading);
		if (lmode) {
			WRITE(p, "layout (location = 2) %sout lowp vec3 v_color1;\n", shading);
		}

		if (doTexture) {
			WRITE(p, "layout (location = 0) out highp vec3 v_texcoord;\n");
		}

		if (enableFog) {
			// See the fragment shader generator
			WRITE(p, "layout (location = 3) out highp float v_fogdepth;\n");
		}
	} else if (compat.shaderLanguage == HLSL_D3D11 || compat.shaderLanguage == HLSL_D3D9) {
		// Note: These two share some code after this hellishly large if/else.
		if (compat.shaderLanguage == HLSL_D3D11) {
			WRITE(p, "cbuffer base : register(b0) {\n%s};\n", ub_baseStr);
			WRITE(p, "cbuffer lights: register(b1) {\n%s};\n", ub_vs_lightsStr);
			WRITE(p, "cbuffer bones : register(b2) {\n%s};\n", ub_vs_bonesStr);
		} else {
			WRITE(p, "#pragma warning( disable : 3571 )\n");
			if (isModeThrough) {
				WRITE(p, "mat4 u_proj_through : register(c%i);\n", CONST_VS_PROJ_THROUGH);
			} else {
				WRITE(p, "mat4 u_proj : register(c%i);\n", CONST_VS_PROJ);
				// Add all the uniforms we'll need to transform properly.
			}

			if (enableFog) {
				WRITE(p, "vec2 u_fogcoef : register(c%i);\n", CONST_VS_FOGCOEF);
			}
			if (useHWTransform || !hasColor)
				WRITE(p, "vec4 u_matambientalpha : register(c%i);\n", CONST_VS_MATAMBIENTALPHA);  // matambient + matalpha

			if (useHWTransform) {
				// When transforming by hardware, we need a great deal more uniforms...
				WRITE(p, "mat3x4 u_world : register(c%i);\n", CONST_VS_WORLD);
				WRITE(p, "mat3x4 u_view : register(c%i);\n", CONST_VS_VIEW);
				if (doTextureTransform)
					WRITE(p, "mat3x4 u_texmtx : register(c%i);\n", CONST_VS_TEXMTX);
				if (enableBones) {
#ifdef USE_BONE_ARRAY
					WRITE(p, "mat3x4 u_bone[%i] : register(c%i);\n", numBones, CONST_VS_BONE0);
#else
					for (int i = 0; i < numBoneWeights; i++) {
						WRITE(p, "mat3x4 u_bone%i : register(c%i);\n", i, CONST_VS_BONE0 + i * 3);
					}
#endif
				}
				if (doTexture) {
					WRITE(p, "vec4 u_uvscaleoffset : register(c%i);\n", CONST_VS_UVSCALEOFFSET);
				}
				for (int i = 0; i < 4; i++) {
					if (doLight[i] != LIGHT_OFF) {
						// This is needed for shade mapping
						WRITE(p, "vec3 u_lightpos%i : register(c%i);\n", i, CONST_VS_LIGHTPOS + i);
					}
					if (doLight[i] == LIGHT_FULL) {
						GELightType type = static_cast<GELightType>(id.Bits(VS_BIT_LIGHT0_TYPE + 4 * i, 2));
						GELightComputation comp = static_cast<GELightComputation>(id.Bits(VS_BIT_LIGHT0_COMP + 4 * i, 2));

						if (type != GE_LIGHTTYPE_DIRECTIONAL)
							WRITE(p, "vec3 u_lightatt%i : register(c%i);\n", i, CONST_VS_LIGHTATT + i);

						if (type == GE_LIGHTTYPE_SPOT || type == GE_LIGHTTYPE_UNKNOWN) {
							WRITE(p, "vec3 u_lightdir%i : register(c%i);\n", i, CONST_VS_LIGHTDIR + i);
							WRITE(p, "vec4 u_lightangle_spotCoef%i : register(c%i);\n", i, CONST_VS_LIGHTANGLE_SPOTCOEF + i);
						}
						WRITE(p, "vec3 u_lightambient%i : register(c%i);\n", i, CONST_VS_LIGHTAMBIENT + i);
						WRITE(p, "vec3 u_lightdiffuse%i : register(c%i);\n", i, CONST_VS_LIGHTDIFFUSE + i);

						if (comp == GE_LIGHTCOMP_BOTH) {
							WRITE(p, "vec3 u_lightspecular%i : register(c%i);\n", i, CONST_VS_LIGHTSPECULAR + i);
						}
					}
				}
				if (enableLighting) {
					WRITE(p, "vec4 u_ambient : register(c%i);\n", CONST_VS_AMBIENT);
					if ((matUpdate & 2) == 0 || !hasColor)
						WRITE(p, "vec3 u_matdiffuse : register(c%i);\n", CONST_VS_MATDIFFUSE);
					// if ((matUpdate & 4) == 0)
					WRITE(p, "vec4 u_matspecular : register(c%i);\n", CONST_VS_MATSPECULAR);  // Specular coef is contained in alpha
					WRITE(p, "vec3 u_matemissive : register(c%i);\n", CONST_VS_MATEMISSIVE);
				}
			}

			if (!isModeThrough) {
				WRITE(p, "vec4 u_depthRange : register(c%i);\n", CONST_VS_DEPTHRANGE);
				WRITE(p, "vec4 u_cullRangeMin : register(c%i);\n", CONST_VS_CULLRANGEMIN);
				WRITE(p, "vec4 u_cullRangeMax : register(c%i);\n", CONST_VS_CULLRANGEMAX);
			}
		}

		// And the "varyings".
		if (useHWTransform) {
			WRITE(p, "struct VS_IN {                              \n");
			if ((doSpline || doBezier) && compat.shaderLanguage == HLSL_D3D11) {
				WRITE(p, "  uint instanceId : SV_InstanceID;\n");
			}
			if (enableBones) {
				WRITE(p, "  %s", boneWeightAttrDeclHLSL[numBoneWeights]);
			}
			if (doTexture && hasTexcoord) {
				WRITE(p, "  vec2 texcoord : TEXCOORD0;\n");
			}
			if (hasColor) {
				WRITE(p, "  vec4 color0 : COLOR0;\n");
			}
			if (hasNormal) {
				WRITE(p, "  vec3 normal : NORMAL;\n");
			}
			WRITE(p, "  vec3 position : POSITION;\n");
			WRITE(p, "};\n");
		} else {
			WRITE(p, "struct VS_IN {\n");
			WRITE(p, "  vec4 position : POSITION;\n");
			if (doTexture && hasTexcoord) {
				if (doTextureTransform && !isModeThrough) {
					texCoordInVec3 = true;
					WRITE(p, "  vec3 texcoord : TEXCOORD0;\n");
				} else
					WRITE(p, "  vec2 texcoord : TEXCOORD0;\n");
			}
			if (hasColor) {
				WRITE(p, "  vec4 color0 : COLOR0;\n");
			}
			// only software transform supplies color1 as vertex data
			if (lmode) {
				WRITE(p, "  vec3 color1 : COLOR1;\n");
			}
			WRITE(p, "};\n");
		}

		WRITE(p, "struct VS_OUT {\n");
		if (doTexture) {
			WRITE(p, "  vec3 v_texcoord : TEXCOORD0;\n");
		}
		const char *colorInterpolation = doFlatShading && compat.shaderLanguage == HLSL_D3D11 ? "nointerpolation " : "";
		WRITE(p, "  %svec4 v_color0    : COLOR0;\n", colorInterpolation);
		if (lmode)
			WRITE(p, "  vec3 v_color1    : COLOR1;\n");

		if (enableFog) {
			WRITE(p, "  float v_fogdepth: TEXCOORD1;\n");
		}
		if (compat.shaderLanguage == HLSL_D3D9) {
			WRITE(p, "  vec4 gl_Position   : POSITION;\n");
		} else {
			WRITE(p, "  vec4 gl_Position   : SV_Position;\n");
			if (vertexRangeCulling && gstate_c.Supports(GPU_SUPPORTS_CLIP_DISTANCE)) {
				WRITE(p, "  float gl_ClipDistance : SV_ClipDistance0;\n");
			}
			if (vertexRangeCulling && gstate_c.Supports(GPU_SUPPORTS_CULL_DISTANCE)) {
				WRITE(p, "  float2 gl_CullDistance : SV_CullDistance0;\n");
			}
		}
		WRITE(p, "};\n");
	} else {
		if (enableBones) {
			const char * const * boneWeightDecl = boneWeightAttrDecl;
			if (!strcmp(compat.attribute, "in")) {
				boneWeightDecl = boneWeightInDecl;
			}
			WRITE(p, "%s", boneWeightDecl[numBoneWeights]);
			*attrMask |= 1 << ATTR_W1;
			if (numBoneWeights >= 5)
				*attrMask |= 1 << ATTR_W2;
		}

		if (useHWTransform)
			WRITE(p, "%s vec3 position;\n", compat.attribute);
		else
			WRITE(p, "%s vec4 position;\n", compat.attribute);  // need to pass the fog coord in w
		*attrMask |= 1 << ATTR_POSITION;

		if (useHWTransform && hasNormal) {
			WRITE(p, "%s mediump vec3 normal;\n", compat.attribute);
			*attrMask |= 1 << ATTR_NORMAL;
		}

		if (doTexture && hasTexcoord) {
			if (!useHWTransform && doTextureTransform && !isModeThrough) {
				WRITE(p, "%s vec3 texcoord;\n", compat.attribute);
				texCoordInVec3 = true;
			} else {
				WRITE(p, "%s vec2 texcoord;\n", compat.attribute);
			}
			*attrMask |= 1 << ATTR_TEXCOORD;
		}
		if (hasColor) {
			WRITE(p, "%s lowp vec4 color0;\n", compat.attribute);
			*attrMask |= 1 << ATTR_COLOR0;
			if (lmode && !useHWTransform) { // only software transform supplies color1 as vertex data
				WRITE(p, "%s lowp vec3 color1;\n", compat.attribute);
				*attrMask |= 1 << ATTR_COLOR1;
			}
		}

		if (isModeThrough) {
			WRITE(p, "uniform mat4 u_proj_through;\n");
			*uniformMask |= DIRTY_PROJTHROUGHMATRIX;
		} else {
			WRITE(p, "uniform mat4 u_proj;\n");
			*uniformMask |= DIRTY_PROJMATRIX;
			// Add all the uniforms we'll need to transform properly.
		}

		if (useHWTransform) {
			// When transforming by hardware, we need a great deal more uniforms...
			// TODO: Use 4x3 matrices where possible. Though probably doesn't matter much.
			WRITE(p, "uniform mat4 u_world;\n");
			WRITE(p, "uniform mat4 u_view;\n");
			*uniformMask |= DIRTY_WORLDMATRIX | DIRTY_VIEWMATRIX;
			if (doTextureTransform) {
				WRITE(p, "uniform mediump mat4 u_texmtx;\n");
				*uniformMask |= DIRTY_TEXMATRIX;
			}
			if (enableBones) {
				for (int i = 0; i < numBoneWeights; i++) {
					WRITE(p, "uniform mat4 u_bone%i;\n", i);
					*uniformMask |= DIRTY_BONEMATRIX0 << i;
				}
			}
			if (doTexture) {
				WRITE(p, "uniform vec4 u_uvscaleoffset;\n");
				*uniformMask |= DIRTY_UVSCALEOFFSET;
			}
			for (int i = 0; i < 4; i++) {
				if (doLight[i] != LIGHT_OFF) {
					// This is needed for shade mapping
					WRITE(p, "uniform vec3 u_lightpos%i;\n", i);
					*uniformMask |= DIRTY_LIGHT0 << i;
				}
				if (doLight[i] == LIGHT_FULL) {
					*uniformMask |= DIRTY_LIGHT0 << i;
					GELightType type = static_cast<GELightType>(id.Bits(VS_BIT_LIGHT0_TYPE + 4 * i, 2));
					GELightComputation comp = static_cast<GELightComputation>(id.Bits(VS_BIT_LIGHT0_COMP + 4 * i, 2));

					if (type != GE_LIGHTTYPE_DIRECTIONAL)
						WRITE(p, "uniform mediump vec3 u_lightatt%i;\n", i);

					if (type == GE_LIGHTTYPE_SPOT || type == GE_LIGHTTYPE_UNKNOWN) {
						WRITE(p, "uniform mediump vec3 u_lightdir%i;\n", i);
						WRITE(p, "uniform mediump vec2 u_lightangle_spotCoef%i;\n", i);
					}
					WRITE(p, "uniform lowp vec3 u_lightambient%i;\n", i);
					WRITE(p, "uniform lowp vec3 u_lightdiffuse%i;\n", i);

					if (comp == GE_LIGHTCOMP_BOTH) {
						WRITE(p, "uniform lowp vec3 u_lightspecular%i;\n", i);
					}
				}
			}
			if (enableLighting) {
				WRITE(p, "uniform lowp vec4 u_ambient;\n");
				*uniformMask |= DIRTY_AMBIENT;
				if ((matUpdate & 2) == 0 || !hasColor) {
					WRITE(p, "uniform lowp vec3 u_matdiffuse;\n");
					*uniformMask |= DIRTY_MATDIFFUSE;
				}
				WRITE(p, "uniform lowp vec4 u_matspecular;\n");  // Specular coef is contained in alpha
				WRITE(p, "uniform lowp vec3 u_matemissive;\n");
				*uniformMask |= DIRTY_MATSPECULAR | DIRTY_MATEMISSIVE;
			}
		}

		if (useHWTransform || !hasColor) {
			WRITE(p, "uniform lowp vec4 u_matambientalpha;\n");  // matambient + matalpha
			*uniformMask |= DIRTY_MATAMBIENTALPHA;
		}
		if (enableFog) {
			WRITE(p, "uniform highp vec2 u_fogcoef;\n");
			*uniformMask |= DIRTY_FOGCOEF;
		}

		if (!isModeThrough) {
			WRITE(p, "uniform highp vec4 u_depthRange;\n");
			WRITE(p, "uniform highp vec4 u_cullRangeMin;\n");
			WRITE(p, "uniform highp vec4 u_cullRangeMax;\n");
			*uniformMask |= DIRTY_DEPTHRANGE | DIRTY_CULLRANGE;
		}

		WRITE(p, "%s%s lowp vec4 v_color0;\n", shading, compat.varying_vs);
		if (lmode) {
			WRITE(p, "%s%s lowp vec3 v_color1;\n", shading, compat.varying_vs);
		}

		if (doTexture) {
			WRITE(p, "%s %s vec3 v_texcoord;\n", compat.varying_vs, highpTexcoord ? "highp" : "mediump");
		}

		if (enableFog) {
			// See the fragment shader generator
			if (highpFog) {
				WRITE(p, "%s highp float v_fogdepth;\n", compat.varying_vs);
			} else {
				WRITE(p, "%s mediump float v_fogdepth;\n", compat.varying_vs);
			}
		}
	}

	// See comment above this function (GenerateVertexShader).
	if (!isModeThrough && gstate_c.Supports(GPU_ROUND_DEPTH_TO_16BIT)) {
		// Apply the projection and viewport to get the Z buffer value, floor to integer, undo the viewport and projection.
		WRITE(p, "\nvec4 depthRoundZVP(vec4 v) {\n");
		WRITE(p, "  float z = v.z / v.w;\n");
		WRITE(p, "  z = z * u_depthRange.x + u_depthRange.y;\n");
		WRITE(p, "  z = floor(z);\n");
		WRITE(p, "  z = (z - u_depthRange.y) / u_depthRange.x;\n");
		WRITE(p, "  return vec4(v.x, v.y, z * v.w, v.w);\n");
		WRITE(p, "}\n\n");
	}

	// Hardware tessellation
	if (doBezier || doSpline) {
		*uniformMask |= DIRTY_BEZIERSPLINE;

		if (compat.shaderLanguage == GLSL_VULKAN) {
			WRITE(p, "struct TessData {\n");
			WRITE(p, "  vec4 pos;\n");
			WRITE(p, "  vec4 tex;\n");
			WRITE(p, "  vec4 col;\n");
			WRITE(p, "};\n");
			WRITE(p, "layout (std430, set = 0, binding = 6) readonly buffer s_tess_data {\n");
			WRITE(p, "  TessData tess_data[];\n");
			WRITE(p, "};\n");

			WRITE(p, "struct TessWeight {\n");
			WRITE(p, "  vec4 basis;\n");
			WRITE(p, "  vec4 deriv;\n");
			WRITE(p, "};\n");
			WRITE(p, "layout (std430, set = 0, binding = 7) readonly buffer s_tess_weights_u {\n");
			WRITE(p, "  TessWeight tess_weights_u[];\n");
			WRITE(p, "};\n");
			WRITE(p, "layout (std430, set = 0, binding = 8) readonly buffer s_tess_weights_v {\n");
			WRITE(p, "  TessWeight tess_weights_v[];\n");
			WRITE(p, "};\n");
		} else if (ShaderLanguageIsOpenGL(compat.shaderLanguage)) {
			WRITE(p, "uniform sampler2D u_tess_points;\n"); // Control Points
			WRITE(p, "uniform sampler2D u_tess_weights_u;\n");
			WRITE(p, "uniform sampler2D u_tess_weights_v;\n");

			WRITE(p, "uniform int u_spline_counts;\n");
		} else if (compat.shaderLanguage == HLSL_D3D11) {
			WRITE(p, "struct TessData {\n");
			WRITE(p, "  vec3 pos; float pad1;\n");
			WRITE(p, "  vec2 tex; vec2 pad2;\n");
			WRITE(p, "  vec4 col;\n");
			WRITE(p, "};\n");
			WRITE(p, "StructuredBuffer<TessData> tess_data : register(t0);\n");

			WRITE(p, "struct TessWeight {\n");
			WRITE(p, "  vec4 basis;\n");
			WRITE(p, "  vec4 deriv;\n");
			WRITE(p, "};\n");
			WRITE(p, "StructuredBuffer<TessWeight> tess_weights_u : register(t1);\n");
			WRITE(p, "StructuredBuffer<TessWeight> tess_weights_v : register(t2);\n");
		} else if (compat.shaderLanguage == HLSL_D3D9) {

		}

		const char *init[3] = { "0.0, 0.0", "0.0, 0.0, 0.0", "0.0, 0.0, 0.0, 0.0" };
		for (int i = 2; i <= 4; i++) {
			// Define 3 types vec2, vec3, vec4
			WRITE(p, "vec%d tess_sample(in vec%d points[16], mat4 weights) {\n", i, i);
			WRITE(p, "  vec%d pos = vec%d(%s);\n", i, i, init[i - 2]);
			for (int v = 0; v < 4; ++v) {
				for (int u = 0; u < 4; ++u) {
					WRITE(p, "  pos += weights[%i][%i] * points[%i];\n", v, u, v * 4 + u);
				}
			}
			WRITE(p, "  return pos;\n");
			WRITE(p, "}\n");
		}

		if (ShaderLanguageIsOpenGL(compat.shaderLanguage) && compat.glslVersionNumber < 130) { // For glsl version 1.10
			WRITE(p, "mat4 outerProduct(vec4 u, vec4 v) {\n");
			WRITE(p, "  return mat4(u * v[0], u * v[1], u * v[2], u * v[3]);\n");
			WRITE(p, "}\n");
		} else if (compat.shaderLanguage == HLSL_D3D9 || compat.shaderLanguage == HLSL_D3D11) {
			WRITE(p, "mat4 outerProduct(vec4 u, vec4 v) {\n");
			WRITE(p, "  return mul((float4x1)v, (float1x4)u);\n");
			WRITE(p, "}\n");
		}

		WRITE(p, "struct Tess {\n");
		WRITE(p, "  vec3 pos;\n");
		if (doTexture)
			WRITE(p, "  vec2 tex;\n");
		WRITE(p, "  vec4 col;\n");
		if (hasNormalTess)
			WRITE(p, "  vec3 nrm;\n");
		WRITE(p, "};\n");

		if (compat.shaderLanguage == HLSL_D3D9 || compat.shaderLanguage == HLSL_D3D11) {
			WRITE(p, "void tessellate(in VS_IN In, out Tess tess) {\n");
			WRITE(p, "  vec3 position = In.position;\n");
			WRITE(p, "  vec3 normal = In.normal;\n");
		} else {
			WRITE(p, "void tessellate(out Tess tess) {\n");
		}
		WRITE(p, "  ivec2 point_pos = ivec2(position.z, normal.z)%s;\n", doBezier ? " * 3" : "");
		WRITE(p, "  ivec2 weight_idx = ivec2(position.xy);\n");

		// Load 4x4 control points
		WRITE(p, "  vec3 _pos[16];\n");
		WRITE(p, "  vec2 _tex[16];\n");
		WRITE(p, "  vec4 _col[16];\n");
		if (compat.coefsFromBuffers) {
			WRITE(p, "  int index;\n");
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					WRITE(p, "  index = (%i + point_pos.y) * int(u_spline_counts) + (%i + point_pos.x);\n", i, j);
					WRITE(p, "  _pos[%i] = tess_data[index].pos.xyz;\n", i * 4 + j);
					if (doTexture && hasTexcoordTess)
						WRITE(p, "  _tex[%i] = tess_data[index].tex.xy;\n", i * 4 + j);
					if (hasColorTess)
						WRITE(p, "  _col[%i] = tess_data[index].col;\n", i * 4 + j);
				}
			}

			// Basis polynomials as weight coefficients
			WRITE(p, "  vec4 basis_u = tess_weights_u[weight_idx.x].basis;\n");
			WRITE(p, "  vec4 basis_v = tess_weights_v[weight_idx.y].basis;\n");
			WRITE(p, "  mat4 basis = outerProduct(basis_u, basis_v);\n");

		} else {
			WRITE(p, "  int index_u, index_v;\n");
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					WRITE(p, "  index_u = (%i + point_pos.x);\n", j);
					WRITE(p, "  index_v = (%i + point_pos.y);\n", i);
					WRITE(p, "  _pos[%i] = %s(u_tess_points, ivec2(index_u, index_v), 0).xyz;\n", i * 4 + j, compat.texelFetch);
					if (doTexture && hasTexcoordTess)
						WRITE(p, "  _tex[%i] = %s(u_tess_points, ivec2(index_u + u_spline_counts, index_v), 0).xy;\n", i * 4 + j, compat.texelFetch);
					if (hasColorTess)
						WRITE(p, "  _col[%i] = %s(u_tess_points, ivec2(index_u + u_spline_counts * 2, index_v), 0).rgba;\n", i * 4 + j, compat.texelFetch);
				}
			}

			// Basis polynomials as weight coefficients
			WRITE(p, "  vec4 basis_u = %s(u_tess_weights_u, %s, 0);\n", compat.texelFetch, "ivec2(weight_idx.x * 2, 0)");
			WRITE(p, "  vec4 basis_v = %s(u_tess_weights_v, %s, 0);\n", compat.texelFetch, "ivec2(weight_idx.y * 2, 0)");
			WRITE(p, "  mat4 basis = outerProduct(basis_u, basis_v);\n");
		}

		// Tessellate
		WRITE(p, "  tess.pos = tess_sample(_pos, basis);\n");
		if (doTexture) {
			if (hasTexcoordTess)
				WRITE(p, "  tess.tex = tess_sample(_tex, basis);\n");
			else
				WRITE(p, "  tess.tex = normal.xy;\n");
		}
		if (hasColorTess)
			WRITE(p, "  tess.col = tess_sample(_col, basis);\n");
		else
			WRITE(p, "  tess.col = u_matambientalpha;\n");
		if (hasNormalTess) {
			if (compat.coefsFromBuffers) {
				// Derivatives as weight coefficients
				WRITE(p, "  vec4 deriv_u = tess_weights_u[weight_idx.x].deriv;\n");
				WRITE(p, "  vec4 deriv_v = tess_weights_v[weight_idx.y].deriv;\n");
			} else {
				// Derivatives as weight coefficients
				WRITE(p, "  vec4 deriv_u = %s(u_tess_weights_u, %s, 0);\n", compat.texelFetch, "ivec2(weight_idx.x * 2 + 1, 0)");
				WRITE(p, "  vec4 deriv_v = %s(u_tess_weights_v, %s, 0);\n", compat.texelFetch, "ivec2(weight_idx.y * 2 + 1, 0)");
			}

			WRITE(p, "  vec3 du = tess_sample(_pos, outerProduct(deriv_u, basis_v));\n");
			WRITE(p, "  vec3 dv = tess_sample(_pos, outerProduct(basis_u, deriv_v));\n");
			WRITE(p, "  tess.nrm = normalize(cross(du, dv));\n");
		}
		WRITE(p, "}\n");
	}

	if (useHWTransform) {
		WRITE(p, "vec3 normalizeOr001(vec3 v) {\n");
		WRITE(p, "   return length(v) == 0.0 ? vec3(0.0, 0.0, 1.0) : normalize(v);\n");
		WRITE(p, "}\n");
	}

	if (ShaderLanguageIsOpenGL(compat.shaderLanguage) || compat.shaderLanguage == GLSL_VULKAN) {
		WRITE(p, "void main() {\n");
	} else if (compat.shaderLanguage == HLSL_D3D9 || compat.shaderLanguage == HLSL_D3D11) {
		WRITE(p, "VS_OUT main(VS_IN In) {\n");
		WRITE(p, "  VS_OUT Out;\n");
		if (doTexture && hasTexcoord) {
			if (texCoordInVec3) {
				WRITE(p, "  vec3 texcoord = In.texcoord;\n");
			} else {
				WRITE(p, "  vec2 texcoord = In.texcoord;\n");
			}
		}
		if (hasColor) {
			WRITE(p, "  vec4 color0 = In.color0;\n");
			if (lmode && !useHWTransform) {
				WRITE(p, "  vec3 color1 = In.color1;\n");
			}
		}
		if (hasNormal) {
			WRITE(p, "  vec3 normal = In.normal;\n");
		}
		if (useHWTransform) {
			WRITE(p, "  vec3 position = In.position;\n");
		} else {
			WRITE(p, "  vec4 position = In.position;\n");
		}
		if (enableBones) {
			WRITE(p, "%s", boneWeightAttrInitHLSL[numBoneWeights]);
		}
	}

	if (!useHWTransform) {
		// Simple pass-through of vertex data to fragment shader
		if (doTexture) {
			if (texCoordInVec3) {
				WRITE(p, "  %sv_texcoord = texcoord;\n", compat.vsOutPrefix);
			} else {
				WRITE(p, "  %sv_texcoord = vec3(texcoord, 1.0);\n", compat.vsOutPrefix);
			}
		}
		if (hasColor) {
			WRITE(p, "  %sv_color0 = color0;\n", compat.vsOutPrefix);
			if (lmode)
				WRITE(p, "  %sv_color1 = color1;\n", compat.vsOutPrefix);
		} else {
			WRITE(p, "  %sv_color0 = u_matambientalpha;\n", compat.vsOutPrefix);
			if (lmode)
				WRITE(p, "  %sv_color1 = splat3(0.0);\n", compat.vsOutPrefix);
		}
		if (enableFog) {
			WRITE(p, "  %sv_fogdepth = position.w;\n", compat.vsOutPrefix);
		}
		if (isModeThrough)	{
			WRITE(p, "  vec4 outPos = mul(u_proj_through, vec4(position.xyz, 1.0));\n");
		} else {
			// The viewport is used in this case, so need to compensate for that.
			if (gstate_c.Supports(GPU_ROUND_DEPTH_TO_16BIT)) {
				WRITE(p, "  vec4 outPos = depthRoundZVP(mul(u_proj, vec4(position.xyz, 1.0)));\n");
			} else {
				WRITE(p, "  vec4 outPos = mul(u_proj, vec4(position.xyz, 1.0));\n");
			}
		}
	} else {
		// Step 1: World Transform / Skinning
		if (!enableBones) {
			if (doBezier || doSpline) {
				// Hardware tessellation
				WRITE(p, "  Tess tess;\n");
				if (compat.shaderLanguage == HLSL_D3D9 || compat.shaderLanguage == HLSL_D3D11) {
					WRITE(p, "  tessellate(In, tess);\n");
				} else {
					WRITE(p, "  tessellate(tess);\n");
				}

				WRITE(p, "  vec3 worldpos = mul(vec4(tess.pos.xyz, 1.0), u_world).xyz;\n");
				if (hasNormalTess) {
					WRITE(p, "  mediump vec3 worldnormal = normalizeOr001(mul(vec4(%stess.nrm, 0.0), u_world).xyz);\n", flipNormalTess ? "-" : "");
				} else {
					WRITE(p, "  mediump vec3 worldnormal = vec3(0.0, 0.0, 1.0);\n");
				}
			} else {
				// No skinning, just standard T&L.
				WRITE(p, "  vec3 worldpos = mul(vec4(position, 1.0), u_world).xyz;\n");
				if (hasNormal)
					WRITE(p, "  mediump vec3 worldnormal = normalizeOr001(mul(vec4(%snormal, 0.0), u_world).xyz);\n", flipNormal ? "-" : "");
				else
					WRITE(p, "  mediump vec3 worldnormal = vec3(0.0, 0.0, 1.0);\n");
			}
		} else {
			static const char *rescale[4] = {"", " * 1.9921875", " * 1.999969482421875", ""}; // 2*127.5f/128.f, 2*32767.5f/32768.f, 1.0f};
			const char *factor = rescale[boneWeightScale];

			static const char * const boneWeightAttr[8] = {
				"w1.x", "w1.y", "w1.z", "w1.w",
				"w2.x", "w2.y", "w2.z", "w2.w",
			};

			const char *boneMatrix = compat.forceMatrix4x4 ? "mat4" : "mat3x4";

			// Uncomment this to screw up bone shaders to check the vertex shader software fallback
			// WRITE(p, "THIS SHOULD ERROR! #error");
			if (numBoneWeights == 1 && ShaderLanguageIsOpenGL(compat.shaderLanguage))
				WRITE(p, "  %s skinMatrix = mul(w1, u_bone0)", boneMatrix);
			else
				WRITE(p, "  %s skinMatrix = mul(w1.x, u_bone0)", boneMatrix);
			for (int i = 1; i < numBoneWeights; i++) {
				const char *weightAttr = boneWeightAttr[i];
				// workaround for "cant do .x of scalar" issue.
				if (ShaderLanguageIsOpenGL(compat.shaderLanguage)) {
					if (numBoneWeights == 1 && i == 0) weightAttr = "w1";
					if (numBoneWeights == 5 && i == 4) weightAttr = "w2";
				}
				WRITE(p, " + mul(%s, u_bone%i)", weightAttr, i);
			}

			WRITE(p, ";\n");

			WRITE(p, "  vec3 skinnedpos = mul(vec4(position, 1.0), skinMatrix).xyz%s;\n", factor);
			WRITE(p, "  vec3 worldpos = mul(vec4(skinnedpos, 1.0), u_world).xyz;\n");

			if (hasNormal) {
				WRITE(p, "  mediump vec3 skinnednormal = mul(vec4(%snormal, 0.0), skinMatrix).xyz%s;\n", flipNormal ? "-" : "", factor);
			} else {
				WRITE(p, "  mediump vec3 skinnednormal = mul(vec4(0.0, 0.0, %s1.0, 0.0), skinMatrix).xyz%s;\n", flipNormal ? "-" : "", factor);
			}
			WRITE(p, "  mediump vec3 worldnormal = normalizeOr001(mul(vec4(skinnednormal, 0.0), u_world).xyz);\n");
		}

		WRITE(p, "  vec4 viewPos = vec4(mul(vec4(worldpos, 1.0), u_view).xyz, 1.0);\n");

		// Final view and projection transforms.
		if (gstate_c.Supports(GPU_ROUND_DEPTH_TO_16BIT)) {
			WRITE(p, "  vec4 outPos = depthRoundZVP(mul(u_proj, viewPos));\n");
		} else {
			WRITE(p, "  vec4 outPos = mul(u_proj, viewPos);\n");
		}

		// TODO: Declare variables for dots for shade mapping if needed.

		const char *ambientStr = (matUpdate & 1) && hasColor ? "color0" : "u_matambientalpha";
		const char *diffuseStr = (matUpdate & 2) && hasColor ? "color0.rgb" : "u_matdiffuse";
		const char *specularStr = (matUpdate & 4) && hasColor ? "color0.rgb" : "u_matspecular.rgb";
		if (doBezier || doSpline) {
			// TODO: Probably, should use hasColorTess but FF4 has a problem with drawing the background.
			ambientStr = (matUpdate & 1) && hasColor ? "tess.col" : "u_matambientalpha";
			diffuseStr = (matUpdate & 2) && hasColor ? "tess.col.rgb" : "u_matdiffuse";
			specularStr = (matUpdate & 4) && hasColor ? "tess.col.rgb" : "u_matspecular.rgb";
		}

		bool diffuseIsZero = true;
		bool specularIsZero = true;
		bool distanceNeeded = false;
		bool anySpots = false;
		if (enableLighting) {
			WRITE(p, "  lowp vec4 lightSum0 = u_ambient * %s + vec4(u_matemissive, 0.0);\n", ambientStr);

			for (int i = 0; i < 4; i++) {
				GELightType type = static_cast<GELightType>(id.Bits(VS_BIT_LIGHT0_TYPE + 4*i, 2));
				GELightComputation comp = static_cast<GELightComputation>(id.Bits(VS_BIT_LIGHT0_COMP + 4*i, 2));
				if (doLight[i] != LIGHT_FULL)
					continue;
				diffuseIsZero = false;
				if (comp == GE_LIGHTCOMP_BOTH)
					specularIsZero = false;
				if (type != GE_LIGHTTYPE_DIRECTIONAL)
					distanceNeeded = true;
				if (type == GE_LIGHTTYPE_SPOT || type == GE_LIGHTTYPE_UNKNOWN)
					anySpots = true;
			}

			if (!specularIsZero) {
				WRITE(p, "  lowp vec3 lightSum1 = splat3(0.0);\n");
			}
			if (!diffuseIsZero) {
				WRITE(p, "  vec3 toLight;\n");
				WRITE(p, "  lowp vec3 diffuse;\n");
			}
			if (distanceNeeded) {
				WRITE(p, "  float distance;\n");
				WRITE(p, "  lowp float lightScale;\n");
			}
			WRITE(p, "  mediump float ldot;\n");
			if (anySpots) {
				WRITE(p, "  lowp float angle;\n");
			}
		}

		// Calculate lights if needed. If shade mapping is enabled, lights may need to be
		// at least partially calculated.
		for (int i = 0; i < 4; i++) {
			if (doLight[i] != LIGHT_FULL)
				continue;

			GELightType type = static_cast<GELightType>(id.Bits(VS_BIT_LIGHT0_TYPE + 4*i, 2));
			GELightComputation comp = static_cast<GELightComputation>(id.Bits(VS_BIT_LIGHT0_COMP + 4*i, 2));

			if (type == GE_LIGHTTYPE_DIRECTIONAL) {
				// We prenormalize light positions for directional lights.
				WRITE(p, "  toLight = u_lightpos%i;\n", i);
			} else {
				WRITE(p, "  toLight = u_lightpos%i - worldpos;\n", i);
				WRITE(p, "  distance = length(toLight);\n");
				WRITE(p, "  toLight /= distance;\n");
			}

			bool doSpecular = comp == GE_LIGHTCOMP_BOTH;
			bool poweredDiffuse = comp == GE_LIGHTCOMP_ONLYPOWDIFFUSE;

			WRITE(p, "  ldot = dot(toLight, worldnormal);\n");
			if (poweredDiffuse) {
				// pow(0.0, 0.0) may be undefined, but the PSP seems to treat it as 1.0.
				// Seen in Tales of the World: Radiant Mythology (#2424.)
				WRITE(p, "  if (u_matspecular.a <= 0.0) {\n");
				WRITE(p, "    ldot = 1.0;\n");
				WRITE(p, "  } else {\n");
				WRITE(p, "    ldot = pow(max(ldot, 0.0), u_matspecular.a);\n");
				WRITE(p, "  }\n");
			}

			const char *timesLightScale = " * lightScale";

			// Attenuation
			switch (type) {
			case GE_LIGHTTYPE_DIRECTIONAL:
				timesLightScale = "";
				break;
			case GE_LIGHTTYPE_POINT:
				WRITE(p, "  lightScale = clamp(1.0 / dot(u_lightatt%i, vec3(1.0, distance, distance*distance)), 0.0, 1.0);\n", i);
				break;
			case GE_LIGHTTYPE_SPOT:
			case GE_LIGHTTYPE_UNKNOWN:
				WRITE(p, "  angle = length(u_lightdir%i) == 0.0 ? 0.0 : dot(normalize(u_lightdir%i), toLight);\n", i, i);
				WRITE(p, "  if (angle >= u_lightangle_spotCoef%i.x) {\n", i);
				WRITE(p, "    lightScale = clamp(1.0 / dot(u_lightatt%i, vec3(1.0, distance, distance*distance)), 0.0, 1.0) * (u_lightangle_spotCoef%i.y <= 0.0 ? 1.0 : pow(angle, u_lightangle_spotCoef%i.y));\n", i, i, i);
				WRITE(p, "  } else {\n");
				WRITE(p, "    lightScale = 0.0;\n");
				WRITE(p, "  }\n");
				break;
			default:
				// ILLEGAL
				break;
			}

			WRITE(p, "  diffuse = (u_lightdiffuse%i * %s) * max(ldot, 0.0);\n", i, diffuseStr);
			if (doSpecular) {
				WRITE(p, "  if (ldot >= 0.0) {\n");
				WRITE(p, "    ldot = dot(normalize(toLight + vec3(0.0, 0.0, 1.0)), worldnormal);\n");
				WRITE(p, "    if (u_matspecular.a <= 0.0) {\n");
				WRITE(p, "      ldot = 1.0;\n");
				WRITE(p, "    } else {\n");
				WRITE(p, "      ldot = pow(max(ldot, 0.0), u_matspecular.a);\n");
				WRITE(p, "    }\n");
				WRITE(p, "    if (ldot > 0.0)\n");
				WRITE(p, "      lightSum1 += u_lightspecular%i * %s * ldot %s;\n", i, specularStr, timesLightScale);
				WRITE(p, "  }\n");
			}
			WRITE(p, "  lightSum0.rgb += (u_lightambient%i * %s.rgb + diffuse)%s;\n", i, ambientStr, timesLightScale);
		}

		if (enableLighting) {
			// Sum up ambient, emissive here.
			if (lmode) {
				WRITE(p, "  %sv_color0 = clamp(lightSum0, 0.0, 1.0);\n", compat.vsOutPrefix);
				// v_color1 only exists when lmode = 1.
				if (specularIsZero) {
					WRITE(p, "  %sv_color1 = splat3(0.0);\n", compat.vsOutPrefix);
				} else {
					WRITE(p, "  %sv_color1 = clamp(lightSum1, 0.0, 1.0);\n", compat.vsOutPrefix);
				}
			} else {
				if (specularIsZero) {
					WRITE(p, "  %sv_color0 = clamp(lightSum0, 0.0, 1.0);\n", compat.vsOutPrefix);
				} else {
					WRITE(p, "  %sv_color0 = clamp(clamp(lightSum0, 0.0, 1.0) + vec4(lightSum1, 0.0), 0.0, 1.0);\n", compat.vsOutPrefix);
				}
			}
		} else {
			// Lighting doesn't affect color.
			if (hasColor) {
				if (doBezier || doSpline)
					WRITE(p, "  %sv_color0 = tess.col;\n", compat.vsOutPrefix);
				else
					WRITE(p, "  %sv_color0 = color0;\n", compat.vsOutPrefix);
			} else {
				WRITE(p, "  %sv_color0 = u_matambientalpha;\n", compat.vsOutPrefix);
			}
			if (lmode)
				WRITE(p, "  %sv_color1 = splat3(0.0);\n", compat.vsOutPrefix);
		}

		bool scaleUV = !isModeThrough && (uvGenMode == GE_TEXMAP_TEXTURE_COORDS || uvGenMode == GE_TEXMAP_UNKNOWN);

		// Step 3: UV generation
		if (doTexture) {
			switch (uvGenMode) {
			case GE_TEXMAP_TEXTURE_COORDS:  // Scale-offset. Easy.
			case GE_TEXMAP_UNKNOWN: // Not sure what this is, but Riviera uses it.  Treating as coords works.
				if (scaleUV) {
					if (hasTexcoord) {
						if (doBezier || doSpline)
							WRITE(p, "  %sv_texcoord = vec3(tess.tex.xy * u_uvscaleoffset.xy + u_uvscaleoffset.zw, 0.0);\n", compat.vsOutPrefix);
						else
							WRITE(p, "  %sv_texcoord = vec3(texcoord.xy * u_uvscaleoffset.xy, 0.0);\n", compat.vsOutPrefix);
					} else {
						WRITE(p, "  %sv_texcoord = splat3(0.0);\n", compat.vsOutPrefix);
					}
				} else {
					if (hasTexcoord) {
						WRITE(p, "  %sv_texcoord = vec3(texcoord.xy * u_uvscaleoffset.xy + u_uvscaleoffset.zw, 0.0);\n", compat.vsOutPrefix);
					} else {
						WRITE(p, "  %sv_texcoord = vec3(u_uvscaleoffset.zw, 0.0);\n", compat.vsOutPrefix);
					}
				}
				break;

			case GE_TEXMAP_TEXTURE_MATRIX:  // Projection mapping.
				{
					std::string temp_tc;
					switch (uvProjMode) {
					case GE_PROJMAP_POSITION:  // Use model space XYZ as source
						temp_tc = "vec4(position, 1.0)";
						break;
					case GE_PROJMAP_UV:  // Use unscaled UV as source
						{
							// prescale is false here.
							if (hasTexcoord) {
								temp_tc = "vec4(texcoord.xy, 0.0, 1.0)";
							} else {
								temp_tc = "vec4(0.0, 0.0, 0.0, 1.0)";
							}
						}
						break;
					case GE_PROJMAP_NORMALIZED_NORMAL:  // Use normalized transformed normal as source
						if (hasNormal)
							temp_tc = StringFromFormat("length(normal) == 0.0 ? vec4(0.0, 0.0, 1.0, 1.0) : vec4(normalize(%snormal), 1.0)", flipNormal ? "-" : "");
						else
							temp_tc = "vec4(0.0, 0.0, 1.0, 1.0)";
						break;
					case GE_PROJMAP_NORMAL:  // Use non-normalized transformed normal as source
						if (hasNormal)
							temp_tc = flipNormal ? "vec4(-normal, 1.0)" : "vec4(normal, 1.0)";
						else
							temp_tc = "vec4(0.0, 0.0, 1.0, 1.0)";
						break;
					}
					// Transform by texture matrix. XYZ as we are doing projection mapping.
					WRITE(p, "  %sv_texcoord = mul(%s, u_texmtx).xyz * vec3(u_uvscaleoffset.xy, 1.0);\n", compat.vsOutPrefix, temp_tc.c_str());
				}
				break;

			case GE_TEXMAP_ENVIRONMENT_MAP:  // Shade mapping - use dots from light sources.
				{
					std::string lightFactor0 = StringFromFormat("(length(u_lightpos%i) == 0.0 ? worldnormal.z : dot(normalize(u_lightpos%i), worldnormal))", ls0, ls0);
					std::string lightFactor1 = StringFromFormat("(length(u_lightpos%i) == 0.0 ? worldnormal.z : dot(normalize(u_lightpos%i), worldnormal))", ls1, ls1);
					WRITE(p, "  %sv_texcoord = vec3(u_uvscaleoffset.xy * vec2(1.0 + %s, 1.0 + %s) * 0.5, 1.0);\n", compat.vsOutPrefix, lightFactor0.c_str(), lightFactor1.c_str());
				}
				break;

			default:
				// ILLEGAL
				break;
			}
		}

		// Compute fogdepth
		if (enableFog)
			WRITE(p, "  %sv_fogdepth = (viewPos.z + u_fogcoef.x) * u_fogcoef.y;\n", compat.vsOutPrefix);
	}

	if (vertexRangeCulling) {
		WRITE(p, "  vec3 projPos = outPos.xyz / outPos.w;\n");
		WRITE(p, "  float projZ = (projPos.z - u_depthRange.z) * u_depthRange.w;\n");
		// Vertex range culling doesn't happen when Z clips, note sign of w is important.
		WRITE(p, "  if (u_cullRangeMin.w <= 0.0 || projZ * outPos.w > -outPos.w) {\n");
		const char *outMin = "projPos.x < u_cullRangeMin.x || projPos.y < u_cullRangeMin.y";
		const char *outMax = "projPos.x > u_cullRangeMax.x || projPos.y > u_cullRangeMax.y";
		WRITE(p, "    if (%s || %s) {\n", outMin, outMax);
		WRITE(p, "      outPos.xyzw = u_cullRangeMax.wwww;\n");
		WRITE(p, "    }\n");
		WRITE(p, "  }\n");
		WRITE(p, "  if (u_cullRangeMin.w <= 0.0) {\n");
		WRITE(p, "    if (projPos.z < u_cullRangeMin.z || projPos.z > u_cullRangeMax.z) {\n");
		WRITE(p, "      outPos.xyzw = u_cullRangeMax.wwww;\n");
		WRITE(p, "    }\n");
		WRITE(p, "  }\n");

		const char *clip0 = compat.shaderLanguage == HLSL_D3D11 ? "" : "[0]";
		const char *cull0 = compat.shaderLanguage == HLSL_D3D11 ? ".x" : "[0]";
		const char *cull1 = compat.shaderLanguage == HLSL_D3D11 ? ".y" : "[1]";
		if (gstate_c.Supports(GPU_SUPPORTS_CLIP_DISTANCE)) {
			// TODO: Not rectangles...
			WRITE(p, "  %sgl_ClipDistance%s = projZ * outPos.w + outPos.w;\n", compat.vsOutPrefix, clip0);
		}
		if (gstate_c.Supports(GPU_SUPPORTS_CULL_DISTANCE)) {
			// Cull any triangle fully outside in the same direction when depth clamp enabled.
			WRITE(p, "  if (u_cullRangeMin.w > 0.0) {\n");
			WRITE(p, "    %sgl_CullDistance%s = projPos.z - u_cullRangeMin.z;\n", compat.vsOutPrefix, cull0);
			WRITE(p, "    %sgl_CullDistance%s = u_cullRangeMax.z - projPos.z;\n", compat.vsOutPrefix, cull1);
			WRITE(p, "  } else {\n");
			WRITE(p, "    %sgl_CullDistance%s = 0.0;\n", compat.vsOutPrefix, cull0);
			WRITE(p, "    %sgl_CullDistance%s = 0.0;\n", compat.vsOutPrefix, cull1);
			WRITE(p, "  }\n");
		}
	}

	// We've named the output gl_Position in HLSL as well.
	WRITE(p, "  %sgl_Position = outPos;\n", compat.vsOutPrefix);

	if (needsZWHack) {
		// See comment in thin3d_vulkan.cpp.
		WRITE(p, "  if (%sgl_Position.z == %sgl_Position.w) %sgl_Position.z *= 0.999999;\n",
			compat.vsOutPrefix, compat.vsOutPrefix, compat.vsOutPrefix);
	}

	if (compat.shaderLanguage == GLSL_VULKAN) {
		WRITE(p, " gl_PointSize = 1.0;\n");
	}
	if (compat.shaderLanguage == HLSL_D3D11 || compat.shaderLanguage == HLSL_D3D9) {
		WRITE(p, "  return Out;\n");
	}
	WRITE(p, "}\n");
	return true;
}
