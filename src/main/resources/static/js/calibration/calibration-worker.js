function loop() {
    postMessage("tick");
    setTimeout(loop, 1000);
}
loop();
