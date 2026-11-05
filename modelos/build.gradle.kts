plugins { id("com.android.asset-pack") }
assetPack {
    packName.set("modelos")
    dynamicDelivery { deliveryType.set("install-time") }
}
tasks.configureEach { dependsOn(rootProject.tasks.named("prepararModelos")) }
