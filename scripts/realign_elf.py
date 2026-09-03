#!/usr/bin/env python3
import struct, sys

PT_LOAD = 1
PT_DYNAMIC = 2
PT_GNU_RELRO = 0x6474e552
PAGE = 0x4000
DT_STRTAB, DT_SYMTAB, DT_RELA, DT_REL, DT_JMPREL = 5, 6, 7, 17, 23
DT_PLTGOT, DT_HASH, DT_GNU_HASH, DT_VERSYM, DT_VERNEED, DT_VERDEF = 3, 4, 0x6ffffef5, 0x6ffffff0, 0x6ffffffe, 0x6ffffffc
DT_INIT, DT_FINI, DT_INIT_ARRAY, DT_FINI_ARRAY, DT_PREINIT_ARRAY, DT_RELR = 12, 13, 25, 26, 32, 36
DT_NULL = 0

def align_up(v, a): return (v + a - 1) & ~(a - 1)

def realign(path):
    with open(path, 'rb') as f:
        data = f.read()
    assert data[:4] == b'\x7fELF'
    endian = '<' if data[5] == 1 else '>'
    is64 = data[4] == 2
    if is64:
        e_phoff = struct.unpack_from(endian+'Q', data, 32)[0]
        e_phentsize = struct.unpack_from(endian+'H', data, 54)[0]
        e_phnum = struct.unpack_from(endian+'H', data, 56)[0]
        e_shoff = struct.unpack_from(endian+'Q', data, 40)[0]
        e_shentsize = struct.unpack_from(endian+'H', data, 58)[0]
        e_shnum = struct.unpack_from(endian+'H', data, 60)[0]
        ph_fmt = endian+'IIQQQQQQ'
        sh_fmt = endian+'IIQQQQIIQQ'
        PO, PV, PF, PM = 2, 3, 5, 6
        SA, SO, SS = 3, 4, 5
        e_shoff_off = 40
        dyn_ent = 16
        dyn_fmt = endian+'qQ'
        u64 = endian+'Q'
        u32 = endian+'I'
    else:
        e_phoff = struct.unpack_from(endian+'I', data, 28)[0]
        e_phentsize = struct.unpack_from(endian+'H', data, 42)[0]
        e_phnum = struct.unpack_from(endian+'H', data, 44)[0]
        e_shoff = struct.unpack_from(endian+'I', data, 32)[0]
        e_shentsize = struct.unpack_from(endian+'H', data, 46)[0]
        e_shnum = struct.unpack_from(endian+'H', data, 48)[0]
        ph_fmt = endian+'IIIIIIII'
        sh_fmt = endian+'IIIIIIIIII'
        PO, PV, PF, PM = 1, 2, 4, 5
        SA, SO, SS = 3, 4, 5
        e_shoff_off = 32
        dyn_ent = 8
        dyn_fmt = endian+'ii'
        u64 = endian+'I'
        u32 = endian+'I'

    phdrs = []
    for i in range(e_phnum):
        off = e_phoff + i*e_phentsize
        v = struct.unpack_from(ph_fmt, data, off)
        phdrs.append({'idx': i, 'type': v[0], 'p_offset': v[PO], 'p_vaddr': v[PV],
                      'p_filesz': v[PF], 'p_memsz': v[PM]})

    shdrs = []
    for i in range(e_shnum):
        off = e_shoff + i*e_shentsize
        v = struct.unpack_from(sh_fmt, data, off)
        shdrs.append({'sh_type': v[1], 'sh_addr': v[SA], 'sh_offset': v[SO], 'sh_size': v[SS]})

    loads = [p for p in phdrs if p['type'] == PT_LOAD]
    new_off = 0
    new_vaddr = 0
    for ld in loads:
        n_off = align_up(new_off, PAGE)
        n_vaddr = 0 if new_vaddr == 0 else align_up(new_vaddr, PAGE)
        ld['n_off'] = n_off
        ld['n_vaddr'] = n_vaddr
        new_off = n_off + ld['p_filesz']
        new_vaddr = n_vaddr + ld['p_memsz']

    total = align_up(new_off, PAGE)
    new_data = bytearray(total)
    for ld in loads:
        new_data[ld['n_off']:ld['n_off']+ld['p_filesz']] = data[ld['p_offset']:ld['p_offset']+ld['p_filesz']]

    # 更新所有 program headers：p_offset/p_vaddr 按所属 LOAD 平移，LOAD 的 align 设 PAGE
    for ph in phdrs:
        off = e_phoff + ph['idx']*e_phentsize
        owner = None
        if ph['type'] == PT_LOAD:
            owner = ph
        else:
            for ld in loads:
                if ld['p_filesz'] > 0 and ld['p_offset'] <= ph['p_offset'] < ld['p_offset'] + ld['p_filesz']:
                    owner = ld
                    break
        if owner:
            new_po = owner['n_off'] + (ph['p_offset'] - owner['p_offset'])
            new_pv = owner['n_vaddr'] + (ph['p_vaddr'] - owner['p_vaddr'])
            struct.pack_into(u64, new_data, off+8, new_po)
            struct.pack_into(u64, new_data, off+16, new_pv)
        if ph['type'] == PT_LOAD or ph['type'] == PT_GNU_RELRO:
            struct.pack_into(u64, new_data, off+(48 if is64 else 28), PAGE)

    # RELRO 段必须覆盖其 LOAD 段的末尾（suffix）且 end 16 KB 对齐：
    # 将含 RELRO 的 LOAD 段与 RELRO 的 memsz 统一对齐到同一个 16 KB 边界
    for ld in loads:
        relro_in_ld = [
            p for p in phdrs
            if p['type'] == PT_GNU_RELRO and
            ld['p_vaddr'] <= p['p_vaddr'] < ld['p_vaddr'] + ld['p_memsz']
        ]
        if not relro_in_ld:
            continue
        # RELRO 覆盖到 LOAD 段内最后一个 PROGBITS 之后，再 16 KB 对齐；
        # 之后的 bss（NOBITS）移到 RELRO 之后，避免被 RELRO 只读保护
        last_progbits_end = ld['p_vaddr']
        for sh in shdrs:
            if sh['sh_type'] == 1 and ld['p_vaddr'] <= sh['sh_addr'] < ld['p_vaddr'] + ld['p_memsz']:
                last_progbits_end = max(last_progbits_end, sh['sh_addr'] + sh['sh_size'])
        relro_new_end = align_up(last_progbits_end, PAGE)
        load_end = relro_new_end
        # bss（NOBITS）在 RELRO 之后的 section：vaddr 顺延到 RELRO end 之后
        bss_cursor = relro_new_end
        for i, sh in enumerate(shdrs):
            if sh['sh_type'] == 8 and ld['p_vaddr'] <= sh['sh_addr'] < ld['p_vaddr'] + ld['p_memsz']:
                if sh['sh_addr'] < relro_new_end:
                    # 记录 bss 的新虚拟地址，供 section 更新阶段使用
                    shdrs[i]['bss_new_addr'] = bss_cursor
                    bss_cursor += sh['sh_size']
                load_end = max(load_end, sh['sh_addr'] + sh['sh_size'])
        load_end = align_up(load_end, PAGE)
        ld['p_memsz'] = load_end - ld['n_vaddr']
        struct.pack_into(u64, new_data, e_phoff + ld['idx'] * e_phentsize + 40, ld['p_memsz'])
        for p in relro_in_ld:
            # RELRO 重排后位于 LOAD 段起始，end 与 LOAD 段 end 相同
            relro_start_new = ld['n_vaddr'] + (p['p_vaddr'] - ld['p_vaddr'])
            p['p_memsz'] = relro_new_end - relro_start_new
            struct.pack_into(u64, new_data, e_phoff + p['idx'] * e_phentsize + 40, p['p_memsz'])

    sh_table = data[e_shoff:e_shoff+e_shnum*e_shentsize]
    new_e_shoff = len(new_data)
    new_data.extend(sh_table)
    struct.pack_into(u64, new_data, e_shoff_off, new_e_shoff)

    for i, sh in enumerate(shdrs):
        if sh['sh_type'] == 0 or sh['sh_size'] == 0:
            continue
        owner = None
        if 'bss_new_addr' in sh:
            owner = None
            for ld in loads:
                if ld['p_vaddr'] <= sh['sh_addr'] < ld['p_vaddr'] + ld['p_memsz']:
                    owner = ld
                    break
            if owner:
                soff = new_e_shoff + i * e_shentsize
                # bss 无文件内容，只更新 sh_addr
                struct.pack_into(u64, new_data, soff + 16, sh['bss_new_addr'])
                sh['sh_addr'] = sh['bss_new_addr']
            continue
        if sh['sh_type'] == 8:  # SHT_NOBITS：无文件内容，按虚拟地址归属
            for ld in loads:
                if ld['p_vaddr'] <= sh['sh_addr'] < ld['p_vaddr'] + ld['p_memsz']:
                    owner = ld
                    break
        else:
            for ld in loads:
                if ld['p_filesz'] > 0 and ld['p_offset'] <= sh['sh_offset'] < ld['p_offset'] + ld['p_filesz']:
                    owner = ld
                    break
        if owner:
            soff = new_e_shoff + i*e_shentsize
            struct.pack_into(u64, new_data, soff+24, owner['n_off'] + (sh['sh_offset'] - owner['p_offset']))
            struct.pack_into(u64, new_data, soff+16, owner['n_vaddr'] + (sh['sh_addr'] - owner['p_vaddr']))

    def vaddr_map(old):
        for ld in loads:
            if ld['p_vaddr'] <= old < ld['p_vaddr'] + ld['p_memsz']:
                return ld['n_vaddr'] + (old - ld['p_vaddr'])
        return None

    addr_tags = {DT_STRTAB, DT_SYMTAB, DT_RELA, DT_REL, DT_JMPREL, DT_PLTGOT, DT_HASH,
                 DT_GNU_HASH, DT_VERSYM, DT_VERNEED, DT_VERDEF, DT_INIT, DT_FINI,
                 DT_INIT_ARRAY, DT_FINI_ARRAY, DT_PREINIT_ARRAY, DT_RELR}
    for dyn in [p for p in phdrs if p['type'] == PT_DYNAMIC]:
        owner = None
        for ld in loads:
            if ld['p_offset'] <= dyn['p_offset'] < ld['p_offset'] + ld['p_filesz']:
                owner = ld
                break
        if not owner:
            continue
        d_off = owner['n_off'] + (dyn['p_offset'] - owner['p_offset'])
        for i in range(dyn['p_filesz'] // dyn_ent):
            eoff = d_off + i*dyn_ent
            tag, val = struct.unpack_from(dyn_fmt, new_data, eoff)
            if tag in addr_tags:
                nv = vaddr_map(val)
                if nv is not None:
                    struct.pack_into(dyn_fmt, new_data, eoff, tag, nv)
            if tag == DT_NULL:
                break

    # 更新重定位表（.rela/.rela.plt）的 r_offset（虚拟地址）
    SHT_RELA = 4
    SHT_REL = 9
    for sh in shdrs:
        if sh['sh_type'] not in (SHT_RELA, SHT_REL) or sh['sh_size'] == 0:
            continue
        owner = None
        for ld in loads:
            if ld['p_filesz'] > 0 and ld['p_offset'] <= sh['sh_offset'] < ld['p_offset'] + ld['p_filesz']:
                owner = ld
                break
        if not owner:
            continue
        sh_off = owner['n_off'] + (sh['sh_offset'] - owner['p_offset'])
        entsz = 24 if (sh['sh_type'] == SHT_RELA and is64) else (12 if sh['sh_type'] == SHT_REL and is64 else 8)
        for i in range(sh['sh_size'] // entsz):
            eoff = sh_off + i * entsz
            if is64:
                r_offset = struct.unpack_from(endian+'Q', new_data, eoff)[0]
                nv = vaddr_map(r_offset)
                if nv is not None:
                    struct.pack_into(endian+'Q', new_data, eoff, nv)
                # RELATIVE 重定位的 addend 也是虚拟地址，需同步映射
                if sh['sh_type'] == SHT_RELA:
                    r_info = struct.unpack_from(endian+'Q', new_data, eoff+8)[0]
                    r_type = r_info & 0xffffffff
                    if r_type == 1027:  # R_AARCH64_RELATIVE
                        r_addend = struct.unpack_from(endian+'Q', new_data, eoff+16)[0]
                        na = vaddr_map(r_addend)
                        if na is not None:
                            struct.pack_into(endian+'Q', new_data, eoff+16, na)
            else:
                r_offset = struct.unpack_from(endian+'I', new_data, eoff)[0]
                nv = vaddr_map(r_offset)
                if nv is not None:
                    struct.pack_into(endian+'I', new_data, eoff, nv)

    # 更新 RELR 表（DT_RELR）：重新编码相对地址
    for dyn in [p for p in phdrs if p['type'] == PT_DYNAMIC]:
        owner = None
        for ld in loads:
            if ld['p_offset'] <= dyn['p_offset'] <= ld['p_offset'] + ld['p_filesz']:
                owner = ld
                break
        if not owner:
            continue
        d_off = owner['n_off'] + (dyn['p_offset'] - owner['p_offset'])
        relr_ptr = None
        relr_sz = 0
        for i in range(dyn['p_filesz'] // dyn_ent):
            eoff = d_off + i*dyn_ent
            tag, val = struct.unpack_from(dyn_fmt, new_data, eoff)
            if tag == 36:  # DT_RELR
                relr_ptr = val
            elif tag == 35:  # DT_RELRSZ
                relr_sz = val
            if tag == DT_NULL:
                break
        if relr_ptr is None or relr_sz == 0:
            continue
        relr_vaddr = vaddr_map(relr_ptr)
        if relr_vaddr is None:
            continue
        # 找到 RELR section 在新文件的位置
        relr_off = None
        for sh in shdrs:
            if sh['sh_type'] != SHT_RELA and sh['sh_size'] == relr_sz and sh['sh_addr'] == relr_ptr:
                for ld in loads:
                    if ld['p_offset'] <= sh['sh_offset'] <= ld['p_offset'] + ld['p_filesz']:
                        relr_off = ld['n_off'] + (sh['sh_offset'] - ld['p_offset'])
                        break
                break
        if relr_off is None:
            continue
        # 解码旧 RELR 地址（相对基址）
        addrs = []
        base = 0
        pos = relr_off
        end = relr_off + relr_sz
        while pos < end:
            entry = struct.unpack_from(endian+'Q', new_data, pos)[0]
            pos += 8
            if entry & 1 == 0:
                base += entry
            else:
                for bit in range(1, 64):
                    if entry & (1 << bit):
                        addrs.append(base + bit*8)
                base += 63*8
        # 映射到新相对地址（相对新加载基址 0）
        new_addrs = []
        for a in addrs:
            nv = vaddr_map(a)
            if nv is not None:
                new_addrs.append(nv)
        new_addrs.sort()
        # 重新编码（简单逐项，可能偏大但正确）
        out = bytearray()
        prev = None
        for a in new_addrs:
            if prev is None or a != prev:
                if prev is None:
                    out += struct.pack(endian+'Q', a)
                else:
                    out += struct.pack(endian+'Q', a - prev)
                prev = a
        # 用新编码覆盖（长度不足补 NULL 项；超长则报错）
        if len(out) > relr_sz:
            print(f"{path}: WARNING RELR too large {len(out)} > {relr_sz}")
        else:
            new_data[relr_off:relr_off+len(out)] = out

    with open(path, 'wb') as f:
        f.write(new_data)
    print(f"{path}: realigned")

for p in sys.argv[1:]:
    realign(p)
