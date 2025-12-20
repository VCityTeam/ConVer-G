#!/bin/sh
cat <<EOF > /usr/share/nginx/html/config.js
window.env = {
  VITE_QUERY_ENDPOINT: "${VITE_QUERY_ENDPOINT}",
  VITE_LOADER_ENDPOINT: "${VITE_LOADER_ENDPOINT}"
};
EOF