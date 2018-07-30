/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   blake2s.h                                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kyork <marvin@42.fr>                       +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2018/05/06 15:18:08 by kyork             #+#    #+#             */
/*   Updated: 2018/07/30 11:53:54 by kyork            ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

#ifndef FT_SSL_BLAKE2S_H
# define FT_SSL_BLAKE2S_H

# include <stdint.h>

# define t_u32 uint32_t
# define t_u8 uint8_t
# define t_s8 int8_t

# define BLAKE2S_BLOCK_SIZE 64
# define BLAKE2S_256_OUTPUT_SIZE 32
# define BLAKE2S_128_OUTPUT_SIZE 16

# define BLAKE2S_KEY_SIZE 32

# define BLAKE2S_FLAG_NORMAL 0

typedef struct			s_blake2s_state {
	t_u32		h[8];
	t_u32		c[2];
	int         out_size;
	t_u8		key[BLAKE2S_BLOCK_SIZE];
	int			keysz;
}						t_blake2s_state;

void blake2s_init_key(struct s_blake2s_state *st, int hash_size,
							t_u8 *key, int key_len);

// always pass 0 for flag. finish() passes a different value.
void					blake2s_block(struct s_blake2s_state *state, t_u8 *block,
							t_u32 flag);
/*
 * The provided buf variable must be BLAKE2S_BLOCK_SIZE bytes long, but
 * only the first 'bufsz' bytes have data in them (the rest will be set
 * to 0).
 *
 * The hashing struct is left in an unusuable state and must be reset
 * before further operations.
 *
 * The hash can be read out of the 'h' variable in little-endian order.
 */
void          blake2s_finish(struct s_blake2s_state *state, t_u8 *buf, int bufsz);

void					blake2s_reset(t_blake2s_state *state);

typedef const t_s8		t_blake2s_sigma[16];

typedef struct			s_blake2s_roundconf {
	int					a;
	int					b;
	int					c;
	int					d;
	int					xi;
	int					yi;
}						t_blake2s_roundconf;

#endif

