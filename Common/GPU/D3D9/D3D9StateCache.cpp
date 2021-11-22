#ifdef _WIN32

#include "Common/GPU/D3D9/D3D9StateCache.h"

namespace DX9 {

DirectXState dxstate;

LPDIRECT3DDEVICE9 pD3Ddevice = nullptr;
LPDIRECT3DDEVICE9EX pD3DdeviceEx = nullptr;

int DirectXState::state_count = 0;

void DirectXState::Initialize() {
	if (initialized)
		return;

	Restore();

	initialized = true;
}

void DirectXState::Restore() {
	int count = 0;

	blend.restore(); count++;
	blendSeparate.restore(); count++;
	blendEquation.restore(); count++;
	blendFunc.restore(); count++;
	blendColor.restore(); count++;

	scissorTest.restore(); count++;
	scissorRect.restore(); count++;

	cullMode.restore(); count++;
	shadeMode.restore(); count++;

	depthTest.restore(); count++;
	depthFunc.restore(); count++;
	depthWrite.restore(); count++;

	colorMask.restore(); count++;

	viewport.restore(); count++;

	alphaTest.restore(); count++;
	alphaTestFunc.restore(); count++;
	alphaTestRef.restore(); count++;

	stencilTest.restore(); count++;
	stencilOp.restore(); count++;
	stencilFunc.restore(); count++;
	stencilMask.restore(); count++;

	dither.restore(); count++;

	texMinFilter.restore(); count++;
	texMagFilter.restore(); count++;
	texMipFilter.restore(); count++;
	texMipLodBias.restore(); count++;
	texMaxMipLevel.restore(); count++;
	texAddressU.restore(); count++;
	texAddressV.restore(); count++;
}

}  // namespace DX9

#endif  // _MSC_VER
