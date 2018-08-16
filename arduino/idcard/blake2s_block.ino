/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   block.c                                            :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kyork <marvin@42.fr>                       +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2018/05/06 15:32:09 by kyork             #+#    #+#             */
/*   Updated: 2018/07/27 12:43:29 by kyork            ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

#include "blake2s.h"

#if __BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__
# define LEU32(buf) (*(t_u32*)(buf))
#elif __BYTE__ORDER == __ORDER_BIG_ENDIAN__
# define LEU32(buf) (__builtin_bswap32(*(t_u32*)(buf)))
#else
# error Unsupported endianness define
#endif

#define READ_U32(ptr) ( ((uint32_t)((ptr)[0])) | (((uint32_t)((ptr)[1])) << 8) | (((uint32_t)((ptr)[2])) << 16) | (((uint32_t)((ptr)[3])) << 24) )

static const t_blake2s_sigma  pg_precomputed[10] = {
	{0, 2, 4, 6, 1, 3, 5, 7, 8, 10, 12, 14, 9, 11, 13, 15},
	{14, 4, 9, 13, 10, 8, 15, 6, 1, 0, 11, 5, 12, 2, 7, 3},
	{11, 12, 5, 15, 8, 0, 2, 13, 10, 3, 7, 9, 14, 6, 1, 4},
	{7, 3, 13, 11, 9, 1, 12, 14, 2, 5, 4, 15, 6, 10, 0, 8},
	{9, 5, 2, 10, 0, 7, 4, 15, 14, 11, 6, 3, 1, 12, 8, 13},
	{2, 6, 0, 8, 12, 10, 11, 3, 4, 7, 15, 1, 13, 5, 14, 9},
	{12, 1, 14, 4, 5, 15, 13, 10, 0, 6, 9, 8, 7, 3, 2, 11},
	{13, 7, 12, 3, 11, 14, 1, 9, 5, 15, 8, 2, 0, 4, 6, 10},
	{6, 14, 11, 0, 15, 9, 3, 8, 12, 13, 1, 10, 2, 7, 4, 5},
	{10, 8, 7, 1, 2, 4, 6, 5, 15, 9, 3, 13, 11, 14, 12, 0},
};

static const uint32_t  pg_blake2s_iv[8] = {
	0x6a09e667,
	0xbb67ae85,
	0x3c6ef372,
	0xa54ff53a,
	0x510e527f,
	0x9b05688c,
	0x1f83d9ab,
	0x5be0cd19,
};

static const struct s_blake2s_roundconf  pg_blake2s_rounds[8] = {
	{0, 4, 8, 12, 0, 4},
	{1, 5, 9, 13, 1, 5},
	{2, 6, 10, 14, 2, 6},
	{3, 7, 11, 15, 3, 7},
	{0, 5, 10, 15, 8, 12},
	{1, 6, 11, 12, 9, 13},
	{2, 7, 8, 13, 10, 14},
	{3, 4, 9, 14, 11, 15},
};

#define BL2s_AA v[pg_blake2s_rounds[roundnum].a]
#define BL2s_BB v[pg_blake2s_rounds[roundnum].b]
#define BL2s_CC v[pg_blake2s_rounds[roundnum].c]
#define BL2s_DD v[pg_blake2s_rounds[roundnum].d]
#define BL2s_XX m[pg_precomputed[sigma_i][pg_blake2s_rounds[roundnum].xi]]
#define BL2s_YY m[pg_precomputed[sigma_i][pg_blake2s_rounds[roundnum].yi]]

extern unsigned int __data_start;
extern unsigned int __data_end;
extern unsigned int __bss_start;
extern unsigned int __bss_end;
extern unsigned int __heap_start;
extern void *__brkval;

static void							blake2s_roundop(int roundnum, int sigma_i, t_u32 *m, t_u32 *v)
{
  /*
  Serial.print("round ");
  Serial.print(BL2s_AA); Serial.print(" ");
  Serial.print(BL2s_BB); Serial.print(" ");
  Serial.print(BL2s_CC); Serial.print(" ");
  Serial.print(BL2s_DD); Serial.print(" ");
  int xi = pg_blake2s_rounds[roundnum].xi;
  int yi = pg_blake2s_rounds[roundnum].yi;
  Serial.print("xi "); Serial.print(xi); Serial.print(" ");
  Serial.print("yi "); Serial.print(pg_blake2s_rounds[roundnum].yi); Serial.print(" ");
  t_blake2s_sigma &sigma = pg_precomputed[sigma_i];
  Serial.print("sigma x "); Serial.print(sigma[xi]); Serial.print(" ");
  Serial.print("sigma y "); Serial.print(pg_precomputed[sigma_i][pg_blake2s_rounds[roundnum].xi]); Serial.print(" ");
  Serial.print(m[sigma[xi]]); Serial.print(" ");
  Serial.print(BL2s_YY); Serial.println();
  */
	BL2s_AA += BL2s_XX;
	BL2s_AA += BL2s_BB;
	BL2s_DD ^= BL2s_AA;
	BL2s_DD = ((BL2s_DD << (32 - 16)) | (BL2s_DD >> 16));
	BL2s_CC += BL2s_DD;
	BL2s_BB ^= BL2s_CC;
	BL2s_BB = ((BL2s_BB << (32 - 12)) | (BL2s_BB >> 12));

	BL2s_AA += BL2s_YY;
	BL2s_AA += BL2s_BB;
	BL2s_DD ^= BL2s_AA;
  BL2s_DD = ((BL2s_DD << (32 - 8)) | (BL2s_DD >> 8));
	BL2s_CC += BL2s_DD;
	BL2s_BB ^= BL2s_CC;
	BL2s_BB = ((BL2s_BB << (32 - 7)) | (BL2s_BB >> 7));
}

static void							blake2s_round(
		int sigma_i, t_u32 *m, t_u32 *v)
{
	blake2s_roundop(0, sigma_i, m, v);
	blake2s_roundop(1, sigma_i, m, v);
	blake2s_roundop(2, sigma_i, m, v);
	blake2s_roundop(3, sigma_i, m, v);
	blake2s_roundop(4, sigma_i, m, v);
	blake2s_roundop(5, sigma_i, m, v);
	blake2s_roundop(6, sigma_i, m, v);
	blake2s_roundop(7, sigma_i, m, v);
}

#define DEBUG_PRINT(...) SERIAL_PRINT(__VA_ARGS__)
#undef DEBUG_PRINT
#define DEBUG_PRINT(...) 

void								blake2s_block(struct s_blake2s_state *state,
		t_u8 *block, t_u32 flag)
{
	t_u32		m[16];
	t_u32		v[16];
	int			i = 0;

	state->c[0] += BLAKE2S_BLOCK_SIZE;
	if (state->c[0] < BLAKE2S_BLOCK_SIZE) {
		state->c[1]++;
	}
 
  for (int i = 0; i < 8; i++) {
    v[i] = state->h[i];
  }
  for (int i = 0; i < 8; i++) {
    v[8 + i] = pg_blake2s_iv[i];
  }
	v[12] ^= state->c[0];
	v[13] ^= state->c[1];
	v[14] ^= flag;
  DEBUG_PRINT("m start: ");
  for (int i = 0; i < 16; i++) {
		m[i] = *(reinterpret_cast<uint32_t*>(&block[i * 4]));
    DEBUG_PRINT(m[i], HEX);
    DEBUG_PRINT(' ');
  }
  DEBUG_PRINT("\nv start: ");
  for (int i = 0; i < 16; i++) {
    DEBUG_PRINT(v[i], HEX);
    DEBUG_PRINT(' ');
  }
  DEBUG_PRINT("\n");
  for (int sigma_i = 0; sigma_i < 10; sigma_i++) {
		blake2s_round(sigma_i, m, v);
  }
  DEBUG_PRINT("block finish: ");
  for (int i = 0; i < 8; i++) {
		state->h[i] ^= v[i] ^ v[i + 8];
    DEBUG_PRINT(state->h[i], HEX);
    DEBUG_PRINT(' ');
  }
  DEBUG_PRINT("\n");
}

void        blake2s_init_key(struct s_blake2s_state *st, int hash_size,
                t_u8 *key, int key_len)
{
  memset(st, 0, sizeof(*st));
  st->out_size = hash_size;
  if (key_len > BLAKE2S_KEY_SIZE)
    abort();
  st->keysz = key_len;
  memset(st->key, 0, BLAKE2S_BLOCK_SIZE);
  memcpy(st->key, key, key_len);
  blake2s_reset(st);
}

void								blake2s_reset(struct s_blake2s_state *st)
{
  for (int i = 0; i < 8; i++)
    st->h[i] = pg_blake2s_iv[i];
  st->c[0] = 0;
  st->c[1] = 0;
	st->h[0] ^= (t_u32)(st->out_size) | (((t_u32)st->keysz) << 8) |
		((t_u32)1 << 16) | ((t_u32)1 << 24);
	if (st->keysz)
		blake2s_block(st, st->key, 0);
}

