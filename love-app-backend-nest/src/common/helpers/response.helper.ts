export function sendResponse(
  dataOrMessage: any,
  dataOrMsg?: any,
  success = true,
) {
  if (typeof dataOrMessage === 'string') {
    return { success, message: dataOrMessage, data: dataOrMsg };
  }
  return { success, message: dataOrMsg ?? 'OK', data: dataOrMessage };
}
