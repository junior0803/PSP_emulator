// Copyright (c) 2021- PPSSPP Project.

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

#pragma once

#include <cstdint>
#include <cstring>
#include <string>
#include <vector>
#include "Common/Common.h"

class PointerWrap;

enum class MemBlockFlags {
	ALLOC = 0x0001,
	SUB_ALLOC = 0x0002,
	WRITE = 0x0004,
	TEXTURE = 0x0008,
	// Not actually logged.
	READ = 0x0800,
	FREE = 0x1000,
	SUB_FREE = 0x2000,

	SKIP_MEMCHECK = 0x00010000,
};
ENUM_CLASS_BITOPS(MemBlockFlags);

struct MemBlockInfo {
	MemBlockFlags flags;
	uint32_t start;
	uint32_t size;
	uint64_t ticks;
	uint32_t pc;
	std::string tag;
	bool allocated;
};

void NotifyMemInfo(MemBlockFlags flags, uint32_t start, uint32_t size, const char *tag, size_t tagLength);
void NotifyMemInfoPC(MemBlockFlags flags, uint32_t start, uint32_t size, uint32_t pc, const char *tag, size_t tagLength);

// This lets us avoid calling strlen on string constants, instead the string length (including null,
// so we have to subtract 1) is computed at compile time.
template<size_t count>
inline void NotifyMemInfo(MemBlockFlags flags, uint32_t start, uint32_t size, const char(&str)[count]) {
	NotifyMemInfo(flags, start, size, str, count - 1);
}

inline void NotifyMemInfo(MemBlockFlags flags, uint32_t start, uint32_t size, const char *str) {
	NotifyMemInfo(flags, start, size, str, strlen(str));
}

std::vector<MemBlockInfo> FindMemInfo(uint32_t start, uint32_t size);
std::vector<MemBlockInfo> FindMemInfoByFlag(MemBlockFlags flags, uint32_t start, uint32_t size);

std::string GetMemWriteTagAt(uint32_t start, uint32_t size);

void MemBlockInfoInit();
void MemBlockInfoShutdown();
void MemBlockInfoDoState(PointerWrap &p);

void MemBlockOverrideDetailed();
void MemBlockReleaseDetailed();
bool MemBlockInfoDetailed();
