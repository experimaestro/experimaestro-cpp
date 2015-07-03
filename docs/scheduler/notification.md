It is possible to notify Experimaestro of the progress of a job. If notification is possible, the
environment variable `XPM_NOTIFICATION_URL` is set by the launcher. It gives the URL that has
to be used to notify that something has happened (for the moment, only a progress
value between 0 and 1 is allowed)

# Bash

With `wget`

```
test "$XPM_NOTIFICATION_URL" && wget --quiet -O /dev/null "$XPM_NOTIFICATION_URL/progress/$progress"
```

# Lua

```lua

-- Create a notification function

xpm_url = os.getenv("XPM_NOTIFICATION_URL")

if xpm_url ~= nil then
  local http = require("socket.http")
  http.TIMEOUT = 0.01 -- set timeout to a low value
  local last_value = -1
  function notify_progress(progress)
    local value = string.format("%s/progress/%.3f", xpm_url, progress)
    if last ~= value then
      local t = coroutine.create(function () http.request(value) end)
      coroutine.resume(t)
      last_value = value
    end
  end
else
  function notify_progress(progress)
  end
end

-- Notifies
notify_progress(0.01)

```