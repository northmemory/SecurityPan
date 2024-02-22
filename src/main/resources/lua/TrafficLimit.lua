if redis.call('exists', KEYS[1]) == 0 then
    redis.call('set', KEYS[1], '1')
    redis.call('expire', KEYS[1], ARGV[1])
    return 1
else
    return redis.call('incr', KEYS[1])
end