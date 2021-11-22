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

#include "Core/HLE/HLE.h"
#include "Core/HLE/FunctionWrappers.h"
#include "Core/HLE/proAdhoc.h"
#include "Core/HLE/sceOpenPSID.h"
#include "Core/MemMapHelpers.h"

SceOpenPSID dummyOpenPSID = { 0x10, 0x02, 0xA3, 0x44, 0x13, 0xF5, 0x93, 0xB0, 0xCC, 0x6E, 0xD1, 0x32, 0x27, 0x85, 0x0F, 0x9D };

void __OpenPSIDInit() {
	// Making sure the ID is unique
	getLocalMac((SceNetEtherAddr*)&dummyOpenPSID);
	return;
}

void __OpenPSIDShutdown() {

	return;
}

static int sceOpenPSIDGetOpenPSID(u32 OpenPSIDPtr)
{
	WARN_LOG(HLE, "UNTESTED %s(%08x)", __FUNCTION__, OpenPSIDPtr);

	if (Memory::IsValidAddress(OpenPSIDPtr))
	{
		Memory::WriteStruct(OpenPSIDPtr, &dummyOpenPSID);
	}
	return 0;
}

static int sceOpenPSIDGetPSID(u32 OpenPSIDPtr,u32 unknown)
{
	WARN_LOG(HLE, "UNTESTED %s(%08x, %08x)", __FUNCTION__, OpenPSIDPtr, unknown);

	if (Memory::IsValidAddress(OpenPSIDPtr))
	{
		Memory::WriteStruct(OpenPSIDPtr, &dummyOpenPSID);
	}
	return 0;
}

/*
Verify if the provided signature is valid for the specified data. The public key
is provided by the system software.

Note:
	The ECDSA algorithm is used to verify a signature.

Parameters:
	pData	Pointer to data the signature has to be verified for. Data length: KIRK_ECDSA_SRC_DATA_LEN.
	pSig	Pointer to the signature to verify. Signature length: KIRK_ECDSA_SIG_LEN.

Returns:
	0 on success, otherwise < 0.
*/
static s32 sceDdrdb_F013F8BF(u32 pDataPtr, u32 pSigPtr) {
	ERROR_LOG(HLE, "UNIMPL %s(%08x, %08x)", __FUNCTION__, pDataPtr, pSigPtr);

	return 0;
}



const HLEFunction sceOpenPSID[] = 
{
	{0XC69BEBCE, &WrapI_U<sceOpenPSIDGetOpenPSID>,   "sceOpenPSIDGetOpenPSID", 'i', "x" },
};

void Register_sceOpenPSID()
{
	RegisterModule("sceOpenPSID", ARRAY_SIZE(sceOpenPSID), sceOpenPSID);
}

// According to https://playstationdev.wiki/pspprxlibraries/5.00/kd/openpsid.xml
// sceOpenPSID_driver library seems to contains a duplicate of sceOpenPSIDGetOpenPSID just like sceOpenPSID library, is this allowed here?
const HLEFunction sceOpenPSID_driver[] =
{
	{0x19D579F0, &WrapI_UU<sceOpenPSIDGetPSID>,      "sceOpenPSIDGetPSID",     'i', "xx" },
	{0XC69BEBCE, &WrapI_U<sceOpenPSIDGetOpenPSID>,   "sceOpenPSIDGetOpenPSID", 'i', "x"  },
};

void Register_sceOpenPSID_driver()
{
	RegisterModule("sceOpenPSID_driver", ARRAY_SIZE(sceOpenPSID_driver), sceOpenPSID_driver);
}
const HLEFunction sceDdrdb[] =
{
	{0xF013F8BF, &WrapI_UU<sceDdrdb_F013F8BF>,   "sceDdrdb_F013F8BF", 'i', "xx" },
};

void Register_sceDdrdb()
{
	RegisterModule("sceDdrdb", ARRAY_SIZE(sceDdrdb), sceDdrdb);
}
