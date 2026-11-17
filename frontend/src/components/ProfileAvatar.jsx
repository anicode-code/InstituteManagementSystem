import { useEffect, useState } from 'react';
import api from '../api/axios';

const DEFAULT_AVATAR = '/default-avatar.svg';

export default function ProfileAvatar({ src, name, size = '', className = '', style }) {
  const classes = ['avatar', size, className].filter(Boolean).join(' ');
  const [imageSrc, setImageSrc] = useState(DEFAULT_AVATAR);

  useEffect(() => {
    let active = true;
    let objectUrl;

    if (!src) {
      setImageSrc(DEFAULT_AVATAR);
    } else if (src.startsWith('/api/')) {
      setImageSrc(DEFAULT_AVATAR);
      api.get(src, { responseType: 'blob' })
        .then(({ data }) => {
          const nextObjectUrl = URL.createObjectURL(data);
          if (active) {
            objectUrl = nextObjectUrl;
            setImageSrc(nextObjectUrl);
          } else {
            URL.revokeObjectURL(nextObjectUrl);
          }
        })
        .catch(() => {
          if (active) setImageSrc(DEFAULT_AVATAR);
        });
    } else {
      setImageSrc(src);
    }

    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [src]);

  return (
    <img
      src={imageSrc}
      alt={name ? `${name} profile picture` : 'Default profile picture'}
      className={classes}
      style={style}
      onError={(event) => {
        event.currentTarget.onerror = null;
        event.currentTarget.src = DEFAULT_AVATAR;
      }}
    />
  );
}
