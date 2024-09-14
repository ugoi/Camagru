/**
 * Check if user is authenticated
 * @returns {boolean}
 */
export function checkUserAuthentication() {
  const token = getCookie("token");
  return token !== "" && token !== undefined && token !== null;
}

/**
 * Get cookie by name
 * @param {string} name
 * @returns {string}
 */
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(";").shift();
}
