# Item-share caption backport

The current fork already formats item names in processGameMessage. The deployed 2026.1.1.0 binary lacked that call. This backport adds it at the same point, before conversion to Discord text.

Use Java 25 and ASM 9.9.1. Run `java -cp asm-9.9.1.jar PatchItemCaption.java baseline.jar classes`, copy baseline.jar to a new file, then run `jar uf candidate.jar -C classes com`. The patcher checks the input SHA-256. Only OutboundToDiscordEvents.class changes. The SMP Test artifact SHA-256 is EEB0EAFC9DB633EACF04FC33029657EE073210AE2D418EB75EABB3DFDE0A0DE9. Repacking may give the archive a different timestamp.

SMP Test relayed an item named `<gradient:#ebebeb:#949494:#787878><b>Windy</b>`. The edited Discord message displayed [Windy] and the embed displayed Windy with its image.
