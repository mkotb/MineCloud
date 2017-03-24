# MineCloud

## What is MineCloud?

MineCloud is a cloud-based solution for large-scale Minecraft networks which require fast, reliable, and scalable deployment system.
This solution is based off of 3 ideas: Scalability, Flexibility, and Reliability.

### Scalability

MineCloud bases RAM usage, player count and TPS to determine the server's performance, and balances player load accordingly by creating 
new servers within your set boundries.

### Flexibility

MineCloud has an extensible API which can be accessed through either JavaScript plugins, or plugins written in Java for the controller.
From the controller, plugins will be able to create and remove servers, watch node statistics such as per-core CPU usage, RAM, and Disk usage, 
and lastly, interact with server metadata. This API can also be accessed through Bungee and Bukkit plugins.

### Reliability

MineCloud uses a fail-safe system designed to prevent network-breaking exceptions and instead, quietly logs the exceptions for later access. And breaks a lot.

## MineCloud's License

MineCloud is licensed under ISC, this is what you can do, must do, and not do with it:

![](http://i.imgur.com/K1Y6whn.png)
