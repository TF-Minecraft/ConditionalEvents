# ConditionalEvents

> Event triggers for TF-Minecraft, maintained from [Ajneb97/ConditionalEvents](https://github.com/Ajneb97/ConditionalEvents).

ConditionalEvents runs actions when Minecraft events meet configured conditions:
commands, item use, joins, deaths, regions and more. Events can run once only or
on a cooldown. This copy tracks upstream 4.80.3 and builds for Paper 1.21.10. It
has no update checker or bStats reporting.

## Documentation

[Project documentation](https://github.com/TF-Minecraft/Docs/blob/main/projects/ConditionalEvents/README.md)

The upstream [ConditionalEvents wiki](https://ajneb97.gitbook.io/conditionalevents)
covers event types, conditions, actions and variables.

## Tests

With Java 21 and the private build inputs prepared by
`.github/scripts/prepare-release.sh`, run `mvn clean verify`.

## License

[MIT](LICENSE), copyright Ajneb97. TF-Minecraft changes are released under the
same license.
