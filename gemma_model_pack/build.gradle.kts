plugins {
    id("com.android.asset-pack")
}

assetPack {
    // Must match the folder/module name and the name you request from code.
    packName.set("gemma_model_pack")

    dynamicDelivery {
        // Best choice for our 1.3 GB Gemma model:
        // the user explicitly downloads it when enabling AI mode.
        deliveryType.set("on-demand")
    }
}