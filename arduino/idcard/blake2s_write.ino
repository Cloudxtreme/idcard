/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   write.c                                            :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kyork <marvin@42.fr>                       +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2018/05/06 15:27:56 by kyork             #+#    #+#             */
/*   Updated: 2018/07/27 12:45:13 by kyork            ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

#include "blake2s.h"
#define MIN(a, b) ({typeof(a)_a=(a);typeof(b)_b=(b);(_a<_b)?_a:_b;})

#define ADJ_SZ(buf, len, adj) buf += adj; len -= adj;

#if __BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__
# define LEU32(buf) (*(t_u32*)(buf))
#elif __BYTE__ORDER == __ORDER_BIG_ENDIAN__
# define LEU32(buf) (__builtin_bswap32(*(t_u32*)(buf)))
#else
# error Unsupported endianness define
#endif

/*
 * important - do NOT write the final block here!
 */
 /*
void			blake2s_write(struct s_blake2s_state *st, t_u8 *buf, int len)
{
	size_t	tmpsz;

	if (st->bufsz)
	{
		tmpsz = MIN((size_t)(BLAKE2S_BLOCK_SIZE - st->bufsz), len);
		memcpy(st->buf + st->bufsz, buf, tmpsz);
		st->bufsz += tmpsz;
    buf += tmpsz;
    len -= tmpsz;
		if (len > 0 && (st->bufsz == BLAKE2S_BLOCK_SIZE)) {
			blake2s_block(st, st->buf, 0);
			st->bufsz = 0;
		}
	}
	while (len > BLAKE2S_BLOCK_SIZE)
	{
		blake2s_block(st, buf, 0);
    buf += BLAKE2S_BLOCK_SIZE;
    len -= BLAKE2S_BLOCK_SIZE;
	}
	memcpy(st->buf, buf, len);
	st->bufsz += len;
}
*/

/*
 * the provided buf must be BLAKE2S_BLOCK_SIZE bytes long.
 *
 * note: finish will wreck the contents of state, and state must be reset
 * to be used again. if we ever want partial hashes, undo the edits to this function.
 *
 * the hash can be read in LEU32 order from state->h
 */
void			blake2s_finish(struct s_blake2s_state *state,
            t_u8 *buf, int bufsz)
{
	int			remaining;

  // zero out any unused bytes
  remaining = BLAKE2S_BLOCK_SIZE - bufsz;
  memset(buf + bufsz, 0, remaining);
	if (state->c[0] < remaining)
		state->c[1]--;
	state->c[0] -= remaining;
	blake2s_block(state, buf, 0xFFFFFFFFUL);
}

void      blake2s_output_hash(struct s_blake2s_state *state, t_u8 *outbuf)
{
#if false
  for (int i = 0; i < 8 && (i * 4 < state->out_size); i++) {
    LEU32(&outbuf[i * 4]) = state->h[i];
  }
#else // _128 only
memcpy(outbuf, state->h, BLAKE2S_128_OUTPUT_SIZE);
  for (int i = 0; i < 4; i++) {
//    LEU32(&outbuf[i * 4]) = state->h[i];
  }
#endif
}

