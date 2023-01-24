config.module.rules.push(
    {
        test: /\.(png|svg|jpg|css)$/,
        type: 'asset/resource'
    }
);
// add resource files needed for the worker
config.module.rules.push(
    {
        test: /\.(onnx|wasm)$/,
        type: 'asset/resource'
    }
);

// turn off the 244k file limit warning
config.performance =  {
   hints: false,
   maxEntrypointSize: 512000,
   maxAssetSize: 512000
}