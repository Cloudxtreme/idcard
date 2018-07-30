/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   ft_memcpy.c                                        :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kyork <marvin@42.fr>                       +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2016/09/21 09:29:10 by kyork             #+#    #+#             */
/*   Updated: 2016/10/04 22:22:43 by kyork            ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

#include <stddef.h>

void	*ft_memcpy(byte *dst, const byte *src, size_t length)
{
	size_t		t;
	byte		*d;
	const byte	*s;

	if (length == 0)
		return (dst);
	d = dst;
	s = src;
	s += length;
	d += length;
	t = length / 1;
	while (t > 0)
	{
		s -= 1;
		d -= 1;
		*d = *s;
		t--;
	}
	return (dst);
}
