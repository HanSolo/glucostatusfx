/* request permission on page load
document.addEventListener('DOMContentLoaded', function() {
 if (!Notification) {
  alert('Desktop notifications not available in your browser. Try Chromium.');
  return;
 }

 if (Notification.permission !== 'granted')
  Notification.requestPermission();
});
*/


function notify(title, msg) {
 if (Notification.permission !== 'granted')
  Notification.requestPermission();
 else {
  var notification = new Notification(title, {
   icon: 'https://github.com/HanSolo/glucostatusfx/raw/main/icon.png',
   body: msg,
  });
  /*
  notification.onclick = function() {
   window.open('http://stackoverflow.com/a/13328397/1269037');
  };
  */
 }
}